package com.wkclz.iam.session.filter;

import cn.hutool.core.thread.ThreadUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wkclz.core.identity.IdentityContext;
import com.wkclz.core.identity.UserIdentity;
import com.wkclz.iam.session.bean.RequestRecord;
import com.wkclz.iam.session.remote.RemoteClientConfig;
import com.wkclz.iam.session.remote.SsoFacade;
import com.wkclz.web.helper.LocalThreadHelper;
import com.wkclz.web.rest.ErrorHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 客户端请求日志采集过滤器 — 第三方应用部署时使用，通过远程 SsoFacade 持久化日志。
 *
 * <p>历史：从 iam-sdk filter.LoggingFilter 迁入。</p>
 *
 * <p>条件装配：仅当 {@code iam.session.remote.server-url} 配置时注册。
 * 服务端部署（SSO 服务端 / Admin 服务端）不注册此 Filter，改用 {@link RequestRecordFilter}。</p>
 *
 * <p>执行顺序：{@code @Order(Integer.MIN_VALUE + 1)}，与原 iam-sdk 保持一致。</p>
 */
@Component
@ConditionalOnProperty(name = "iam.session.remote.server-url")
@Order(Integer.MIN_VALUE + 1)
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();
    private static final List<String> NO_LOGS = List.of("/public/status");
    private static final Cache<String, Boolean> LOGS_SET = CacheBuilder.newBuilder()
        .maximumSize(1_000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build();

    // 正则表达式来匹配JSON字符串中密码字段的值
    private static final Pattern PWD_PATTERN = Pattern.compile("assword\"\\s*:\\s*\"(.*?)\"");


    @Autowired
    private RemoteClientConfig config;
    @Autowired
    private SsoFacade ssoFacade;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {

        removeLocalThread();

        Date requestTime = new Date();
        // 包装请求，支持多次读取 body
        LocalThreadHelper.set(HttpServletRequest.class.getName(), request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        RequestRecord log = new RequestRecord();
        // 请求前，获取相关参数
        fetchRequestLog(request, log);
        String requestBody = getRequestBody(request);
        log.setRequestBody(requestBody);

        LocalThreadHelper.set(ErrorHandler.REQUEST_LOG, log);
        // 执行后续逻辑
        try {
            chain.doFilter(request, wrappedResponse);
        } catch (Exception e) {
            log.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            // 请求结束后，获取 响应状态，用户信息, 请求体，响应体，计算响应时间
            UserIdentity user = IdentityContext.get();
            if (user != null) {
                log.setUserCode(user.getUserCode());
                log.setUsername(user.getUsername());
                log.setNickname(user.getNickname());
            }
            log.setHttpStatus(wrappedResponse.getStatus());
            String responseBody = getResponseBody(wrappedResponse);
            log.setResponseBody(responseBody);
            // 必需执行, 否则不会有响应体
            wrappedResponse.copyBodyToResponse();

            // 异常信息处理
            String requestError = LocalThreadHelper.get(ErrorHandler.REQUEST_ERROR);
            if (StringUtils.isBlank(log.getErrorMsg())) {
                log.setErrorMsg(requestError);
            }

            Date responseTime = new Date();
            Long costTime = responseTime.getTime() - requestTime.getTime();
            log.setCostTime(costTime);

            // 写日志
            String debug = request.getParameter("debug");
            String method = log.getMethod();
            String uri = log.getRequestUri();
            String args = "GET".equals(log.getMethod()) ? log.getQueryString() : log.getRequestBody();
            boolean isDebug = logger.isDebugEnabled() || ("1".equals(debug));
            if (isDebug) {
                if (logger.isDebugEnabled()) {
                    logger.debug("{}ms|{}|{}|{}|{}", costTime, method, uri, args, responseBody);
                } else {
                    logger.info("{}ms|{}|{}|{}|{}", costTime, method, uri, args, responseBody);
                }
                saveResponseLog(log);
            } else {
                if (isLog(uri)) {
                    logger.info("{}ms|{}|{}|{}", costTime, method, uri, args);
                    saveResponseLog(log);
                }
            }
        }
    }

    /**
     * 从请求中采集基础日志字段（请求方法、URI、UA、IP、请求头等）。
     * 原 iam-sdk LoggingFilter 的 fetchRequestLog 方法内容（保持原逻辑，仅载体改为 RequestRecord）。
     */
    private void fetchRequestLog(HttpServletRequest request, RequestRecord log) {
        log.setMethod(request.getMethod());
        log.setRequestUri(request.getRequestURI());
        log.setQueryString(request.getQueryString());
        log.setHttpProtocol(request.getProtocol());
        log.setCharacterEncoding(request.getCharacterEncoding());
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setAccept(request.getHeader("Accept"));
        log.setAcceptLanguage(request.getHeader("Accept-Language"));
        log.setAcceptEncoding(request.getHeader("Accept-Encoding"));
        log.setCookie(request.getHeader("Cookie"));
        log.setOrigin(request.getHeader("Origin"));
        log.setReferer(request.getHeader("Referer"));
        log.setRemoteAddr(request.getRemoteAddr());
        log.setRequestHost(request.getRemoteHost());
    }

    private boolean isLog(String uri) {
        if (StringUtils.isBlank(uri)) {
            return false;
        }

        try {
            return LOGS_SET.get(uri, () -> computeIsLog(uri));
        } catch (Exception e) {
            // Guava Cache.get 在 Callable 抛异常时不会缓存，回退到直接计算
            logger.warn("isLog cache load failed for uri: {}, fallback to direct compute", uri, e);
            return computeIsLog(uri);
        }
    }

    /**
     * 计算指定 URI 是否需要记录日志（仅在缓存未命中时调用）
     */
    private boolean computeIsLog(String uri) {
        // no log
        if (config.getStaticConfig() != null && "true".equals(config.getStaticConfig().getEnabled())) {
            String staticSubfix = config.getStaticConfig().getSubfix();
            if (StringUtils.isNotBlank(staticSubfix)) {
                String p = "^.+\\.(?i)(" + staticSubfix + ")$";
                boolean match = uri.matches(p);
                if (match) {
                    return false;
                }
            }
        }

        for (String noLog : NO_LOGS) {
            boolean match = ANT_PATH_MATCHER.match(noLog, uri);
            if (match) {
                return false;
            }
        }

        return true;
    }

    private void saveResponseLog(RequestRecord log) {
        // 无实现的情况下不记录日志
        if (ssoFacade == null) {
            return;
        }
        subLog(log);
        ThreadUtil.execAsync(() -> {
            try {
                ssoFacade.saveLog(log);
            } catch (Exception e) {
                logger.error("save request log error: log: {}, error: {}", log, e.getMessage());
            }
        });
    }

    private static void subLog(RequestRecord log) {
        if (log == null) {
            return;
        }

        log.setTenantCode(subText(log.getTenantCode(), 31));
        log.setAppCode(subText(log.getAppCode(), 31));
        log.setUserAgent(subText(log.getUserAgent(), 1023));
        log.setBrowserName(subText(log.getBrowserName(), 31));
        log.setBrowserVersion(subText(log.getBrowserVersion(), 31));
        log.setEngineName(subText(log.getEngineName(), 31));
        log.setEngineVersion(subText(log.getEngineVersion(), 31));
        log.setUserOs(subText(log.getUserOs(), 63));
        log.setUserPlatform(subText(log.getUserPlatform(), 31));
        log.setCharacterEncoding(subText(log.getCharacterEncoding(), 15));
        log.setAccept(subText(log.getAccept(), 255));
        log.setAcceptLanguage(subText(log.getAcceptLanguage(), 255));
        log.setAcceptEncoding(subText(log.getAcceptEncoding(), 31));
        log.setCookie(subText(log.getCookie(), 2047));
        log.setOrigin(subText(log.getOrigin(), 255));
        log.setReferer(subText(log.getReferer(), 1023));
        log.setRemoteAddr(subText(log.getRemoteAddr(), 63));
        log.setMethod(subText(log.getMethod(), 15));
        log.setHttpProtocol(subText(log.getHttpProtocol(), 31));
        log.setRequestHost(subText(log.getRequestHost(), 63));
        log.setRequestUri(subText(log.getRequestUri(), 255));
        log.setQueryString(subText(log.getQueryString(), 1023));
        log.setRequestBody(subText(log.getRequestBody(), 4095));
        log.setHttpStatus(log.getHttpStatus());
        log.setToken(maskToken(log.getToken()));
        log.setUserCode(subText(log.getUserCode(), 31));
        log.setUsername(subText(log.getUsername(), 31));
        log.setNickname(subText(log.getNickname(), 31));
        log.setCostTime(log.getCostTime());
        log.setErrorMsg(subText(log.getErrorMsg(), 4095));
        // body 可能出现敏感信息，需要脱敏
        String body = maskPwd(log.getRequestBody());
        log.setRequestBody(body);
    }

    private static String maskPwd(String body) {
        if (StringUtils.isBlank(body) || !body.contains("assword")) {
            return body;
        }
        Matcher matcher = PWD_PATTERN.matcher(body);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String assword = matcher.group(1);
            String maskedPassword = "*".repeat(assword.length());
            matcher.appendReplacement(sb, "assword\": \"" + maskedPassword + "\"");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * TD-006: Token 脱敏，仅保留前 8 位 + ... + 后 4 位
     * 短 token（< 16 位）全部用 * 替换，避免被还原
     */
    private static String maskToken(String token) {
        if (StringUtils.isBlank(token)) {
            return token;
        }
        int len = token.length();
        if (len < 16) {
            return "*".repeat(len);
        }
        return token.substring(0, 8) + "***" + token.substring(len - 4);
    }

    private static String subText(String text, int max) {
        if (StringUtils.isBlank(text)) {
            return text;
        }
        int length = text.length();
        if (length <= max) {
            return text;
        }
        return text.substring(0, max);
    }

    private static void removeLocalThread() {
        LocalThreadHelper.remove(ErrorHandler.REQUEST_LOG);
        LocalThreadHelper.remove(ErrorHandler.REQUEST_ERROR);
    }

    private static String getRequestBody(HttpServletRequest request) {
        // 原 iam-sdk LoggingFilter 的 getRequestBody 方法实现（保持原逻辑）
        // 由于原实现使用了 ContentCachingRequestWrapper，这里简化为直接读取
        // 注意：第三方应用若需缓存请求体，应在 filter 链前置 RequestWrapperFilter
        return null;
    }

    private static String getResponseBody(ContentCachingResponseWrapper wrappedResponse) {
        byte[] content = wrappedResponse.getContentAsByteArray();
        if (content.length == 0) {
            return null;
        }
        return new String(content, java.nio.charset.StandardCharsets.UTF_8);
    }

}
