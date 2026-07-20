package com.wkclz.iam.sdk.filter;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wkclz.iam.sdk.config.IamSdkConfig;
import com.wkclz.iam.sdk.facade.SsoFacade;
import com.wkclz.iam.sdk.helper.SessionHelper;
import com.wkclz.iam.sdk.bean.RequestLog;
import com.wkclz.iam.sdk.bean.UserSession;
import com.wkclz.web.helper.IpHelper;
import com.wkclz.web.helper.LocalThreadHelper;
import com.wkclz.web.rest.ErrorHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
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
    private IamSdkConfig config;
    @Autowired(required = false)
    private SsoFacade ssoFacade;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {

        removeLocalThread();

        Date requestTime = new Date();
        // 包装请求，支持多次读取 body
        LocalThreadHelper.set(HttpServletRequest.class.getName(), request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        RequestLog log = new RequestLog();
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
            UserSession user = SessionHelper.getUserSession(request);
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
        if ("true".equals(config.getStaticEnabled())) {
            String staticSubfix = config.getStaticSubfix();
            if (StringUtils.isNotBlank(staticSubfix)) {
                String p = "^.+\\.(?i)("+staticSubfix+")$";
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

    private void saveResponseLog(RequestLog log) {
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

    private static void subLog(RequestLog log) {
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

}
