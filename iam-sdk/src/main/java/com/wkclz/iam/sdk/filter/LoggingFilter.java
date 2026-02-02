package com.wkclz.iam.sdk.filter;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.wkclz.iam.sdk.config.IamSdkConfig;
import com.wkclz.iam.sdk.facade.SsoFacade;
import com.wkclz.iam.sdk.helper.SessionHelper;
import com.wkclz.iam.sdk.model.RequestLog;
import com.wkclz.iam.sdk.model.UserSession;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(Integer.MIN_VALUE + 1)
public class LoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();
    private static final List<String> NO_LOGS = List.of("/public/status");
    private static final Map<String, Boolean> LOGS_SET = new HashMap<>();

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



    private void fetchRequestLog(HttpServletRequest request, RequestLog log) {
        log.setHttpStatus(200);
        log.setMethod(request.getMethod());
        log.setRequestHost(request.getRemoteHost());
        log.setRequestUri(request.getRequestURI());
        log.setQueryString(request.getQueryString());

        log.setUserAgent(request.getHeader("User-Agent"));
        log.setHttpProtocol(request.getProtocol());
        log.setCharacterEncoding(request.getCharacterEncoding());
        log.setAccept(request.getHeader("Accept"));
        log.setAcceptLanguage(request.getHeader("Accept-Language"));
        log.setAcceptEncoding(request.getHeader("Accept-Encoding"));
        log.setCookie(request.getHeader("Cookie"));
        log.setOrigin(request.getHeader("Origin"));
        log.setReferer(request.getHeader("Referer"));

        log.setRemoteAddr(IpHelper.getOriginIp(request));
        log.setToken(SessionHelper.getToken(request));
        log.setTenantCode(SessionHelper.getTenantCode());
        log.setAppCode(SessionHelper.getAppCode(request));

        if (log.getUserAgent() != null) {
            UserAgent ua = UserAgentUtil.parse(log.getUserAgent());
            log.setBrowserName(ua.getBrowser() == null? null:ua.getBrowser().toString());
            log.setBrowserVersion(ua.getVersion());
            log.setEngineName(ua.getEngine() == null ? null:ua.getEngine().toString());
            log.setEngineVersion(ua.getEngineVersion());
            log.setUserOs(ua.getOs() == null ? null:ua.getOs().toString());
            log.setUserPlatform(ua.getPlatform() == null ? null:ua.getPlatform().toString());
        }
    }

    private String getRequestBody(ServletRequest request) throws IOException {
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("multipart/form-data")) {
            return null;
        }
        if (!(request instanceof EagerContentCachingRequestWrapper requestWrapper)) {
            return null;
        }
        // 只有在非 form-data 时才需要提前缓存，减少因文件上传导致的内存压力
        requestWrapper.makeBodyCache();
        byte[] body = requestWrapper.getContentAsByteArray();
        if (body.length == 0) {
            return "";
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private String getResponseBody(ContentCachingResponseWrapper wrappedResponse) {
        String contentType = wrappedResponse.getContentType();
        if (contentType == null || !contentType.toLowerCase().contains("application/json")) {
            return null;
        }
        byte[] body = wrappedResponse.getContentAsByteArray();
        if (body.length == 0) {
            return "";
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private synchronized boolean isLog(String uri) {
        if (StringUtils.isBlank(uri)) {
            return false;
        }

        // 缓存
        Boolean a = LOGS_SET.get(uri);
        if (a != null) {
            return a;
        }

        // no log
        if ("true".equals(config.getStaticEnabled())) {
            String staticSubfix = config.getStaticSubfix();
            if (StringUtils.isNotBlank(staticSubfix)) {
                String p = "^.+\\.(?i)("+staticSubfix+")$";
                boolean match = uri.matches(p);
                if (match) {
                    LOGS_SET.put(uri, false);
                    return false;
                }
            }
        }

        for (String noLog : NO_LOGS) {
            boolean match = ANT_PATH_MATCHER.match(noLog, uri);
            if (match) {
                LOGS_SET.put(uri, false);
                return false;
            }
        }

        LOGS_SET.put(uri, true);
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
        log.setToken(subText(log.getToken(), 511));
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