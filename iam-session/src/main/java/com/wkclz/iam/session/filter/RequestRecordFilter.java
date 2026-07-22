package com.wkclz.iam.session.filter;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wkclz.core.identity.IdentityContext;
import com.wkclz.iam.session.bean.RequestRecord;
import com.wkclz.iam.session.spi.RequestRecordHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 请求日志采集过滤器 — 在请求执行前后采集请求/响应信息，异步写入请求日志。
 *
 * <p>统一支持服务端部署和客户端部署：
 * <ul>
 *   <li>服务端：通过 {@link RequestRecordHandler} SPI 持久化到本地数据库（iam-sso 提供 RequestRecordHandlerImpl）</li>
 *   <li>客户端：通过 {@code RemoteRequestRecordHandler} 持久化到远程 SSO 服务端（iam-session remote 子包提供）</li>
 * </ul>
 *
 * <p>执行顺序：
 * <ol>
 *   <li>记录开始时间，包装 request/response</li>
 *   <li>调用下层过滤器链（{@link SessionAuthFilter} → 业务 Controller）</li>
 *   <li>请求结束后采集响应信息、用户身份、计算耗时</li>
 *   <li>字段截断 + 密码脱敏后通过 {@link RequestRecordHandler} SPI 异步持久化</li>
 * </ol>
 *
 * <p>Order 优先级高于 {@link SessionAuthFilter}（无显式 Order），作为最外层包装过滤器。</p>
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class RequestRecordFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestRecordFilter.class);

    private static final Pattern PWD_PATTERN = Pattern.compile("assword\"\\s*:\\s*\"(.*?)\"");

    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    /**
     * 不记录日志的 URI 模式
     */
    private static final List<String> NO_LOGS = List.of("/public/status", "/public/health");

    /**
     * 静态资源后缀正则（匹配的不记录日志）
     */
    private static final Pattern STATIC_RESOURCE_PATTERN = Pattern.compile(
        "^.+\\.(?i)(js|css|jpg|png|mp3|html|htm|jpeg|ttf|woff|ico|woff2|map)$");

    /**
     * URI 日志过滤缓存（性能优化：避免每次请求都匹配正则和 AntPath）
     */
    private static final Cache<String, Boolean> LOGS_SET = CacheBuilder.newBuilder()
        .maximumSize(1_000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build();

    @Autowired
    private RequestRecordHandler requestRecordHandler;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // 包装 request（缓存请求体）和 response（缓存响应体）
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 0);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        String errorMsg = null;
        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } catch (Exception e) {
            // 捕获异常信息，不影响上层异常处理
            errorMsg = e.getMessage();
            log.debug("Request processing error: {}", e.getMessage());
            throw e;
        } finally {
            // 收集日志信息（在 SessionAuthFilter 的 finally 已清理 IdentityContext 后执行）
            RequestRecord logData = collectLogData(wrappedRequest, wrappedResponse, startTime, errorMsg);
            // 必须执行，否则客户端收不到响应体
            wrappedResponse.copyBodyToResponse();
            // 异步持久化
            persistAsync(logData, request);
        }
    }

    /**
     * 收集请求/响应日志数据。
     * <p>用户信息从请求属性获取——由 {@link SessionAuthFilter} 在设置完 IdentityContext 后
     * 存入请求属性，然后在 finally 中清理 IdentityContext。</p>
     */
    private RequestRecord collectLogData(ContentCachingRequestWrapper request,
                                         ContentCachingResponseWrapper response,
                                         long startTime,
                                         String errorMsg) {
        RequestRecord record = new RequestRecord();

        // 基础请求信息
        record.setMethod(request.getMethod());
        record.setRequestUri(request.getRequestURI());
        record.setQueryString(request.getQueryString());
        record.setHttpProtocol(request.getProtocol());
        record.setCharacterEncoding(request.getCharacterEncoding());

        // 请求体（JSON 内容）
        String requestBody = getCachedRequestBody(request);
        record.setRequestBody(maskPassword(requestBody));

        // 请求头
        record.setUserAgent(request.getHeader("User-Agent"));
        record.setAccept(request.getHeader("Accept"));
        record.setAcceptLanguage(request.getHeader("Accept-Language"));
        record.setAcceptEncoding(request.getHeader("Accept-Encoding"));
        record.setCookie(request.getHeader("Cookie"));
        record.setOrigin(request.getHeader("Origin"));
        record.setReferer(request.getHeader("Referer"));

        // 浏览器信息
        if (record.getUserAgent() != null) {
            UserAgent ua = UserAgentUtil.parse(record.getUserAgent());
            record.setBrowserName(ua.getBrowser() == null ? null : ua.getBrowser().toString());
            record.setBrowserVersion(ua.getVersion());
            record.setEngineName(ua.getEngine() == null ? null : ua.getEngine().toString());
            record.setEngineVersion(ua.getEngineVersion());
            record.setUserOs(ua.getOs() == null ? null : ua.getOs().toString());
            record.setUserPlatform(ua.getPlatform() == null ? null : ua.getPlatform().toString());
        }

        // 网络信息
        record.setRemoteAddr(getClientIp(request));
        record.setRequestHost(request.getRemoteHost());

        // 应用/租户
        record.setAppCode(IdentityContext.getAppCode());
        record.setTenantCode(IdentityContext.getTenantCode());

        // Token（脱敏后存储）
        String rawToken = extractToken(request);
        record.setToken(maskToken(rawToken));

        // 用户信息（从请求属性获取，由 SessionAuthFilter 在清理 IdentityContext 前存入）
        record.setUserCode(IdentityContext.getUserCode());
        record.setUsername(IdentityContext.getUsername());
        record.setNickname(IdentityContext.getNickname());

        // 响应信息
        record.setHttpStatus(response.getStatus());
        String responseBody = getCachedResponseBody(response);
        record.setResponseBody(maskPassword(responseBody));

        // 耗时
        record.setCostTime(System.currentTimeMillis() - startTime);

        // 异常信息
        record.setErrorMsg(errorMsg);

        // 字段截断（防止数据库字段超长）
        truncateFields(record);

        return record;
    }

    /**
     * 异步持久化请求日志。
     */
    private void persistAsync(RequestRecord record, HttpServletRequest request) {
        String uri = record.getRequestUri();
        if (uri != null && !isLog(uri)) {
            return;
        }

        // debug 模式：输出详细日志（含响应体）
        String debug = request.getParameter("debug");
        boolean isDebug = log.isDebugEnabled() || ("1".equals(debug));
        String method = record.getMethod();
        String args = "GET".equals(method) ? record.getQueryString() : record.getRequestBody();
        if (isDebug) {
            if (log.isDebugEnabled()) {
                log.debug("{}ms|{}|{}|{}|{}", record.getCostTime(), method, uri, args, record.getResponseBody());
            } else {
                log.info("{}ms|{}|{}|{}|{}", record.getCostTime(), method, uri, args, record.getResponseBody());
            }
        } else {
            log.info("{}ms|{}|{}|{}", record.getCostTime(), method, uri, args);
        }

        CompletableFuture.runAsync(() -> {
            try {
                requestRecordHandler.handle(record);
            } catch (Exception e) {
                log.warn("Failed to persist request log: uri={}, error={}", uri, e.getMessage());
            }
        });
    }

    // ========== 工具方法 ==========

    /**
     * 判断指定 URI 是否需要记录日志（带 Guava Cache 性能优化）。
     */
    private boolean isLog(String uri) {
        if (StringUtils.isBlank(uri)) {
            return false;
        }
        try {
            return LOGS_SET.get(uri, () -> computeIsLog(uri));
        } catch (Exception e) {
            log.warn("isLog cache load failed for uri: {}, fallback to direct compute", uri, e);
            return computeIsLog(uri);
        }
    }

    /**
     * 计算指定 URI 是否需要记录日志（仅在缓存未命中时调用）。
     */
    private boolean computeIsLog(String uri) {
        // 静态资源不记录
        if (STATIC_RESOURCE_PATTERN.matcher(uri).matches()) {
            return false;
        }
        // 排除列表不记录
        for (String noLog : NO_LOGS) {
            if (ANT_PATH_MATCHER.match(noLog, uri)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 字段截断 — 将所有字段截断到数据库字段长度，防止插入失败。
     */
    private static void truncateFields(RequestRecord record) {
        if (record == null) {
            return;
        }
        record.setTenantCode(subText(record.getTenantCode(), 31));
        record.setAppCode(subText(record.getAppCode(), 31));
        record.setUserAgent(subText(record.getUserAgent(), 1023));
        record.setBrowserName(subText(record.getBrowserName(), 31));
        record.setBrowserVersion(subText(record.getBrowserVersion(), 31));
        record.setEngineName(subText(record.getEngineName(), 31));
        record.setEngineVersion(subText(record.getEngineVersion(), 31));
        record.setUserOs(subText(record.getUserOs(), 63));
        record.setUserPlatform(subText(record.getUserPlatform(), 31));
        record.setCharacterEncoding(subText(record.getCharacterEncoding(), 15));
        record.setAccept(subText(record.getAccept(), 255));
        record.setAcceptLanguage(subText(record.getAcceptLanguage(), 255));
        record.setAcceptEncoding(subText(record.getAcceptEncoding(), 31));
        record.setCookie(subText(record.getCookie(), 2047));
        record.setOrigin(subText(record.getOrigin(), 255));
        record.setReferer(subText(record.getReferer(), 1023));
        record.setRemoteAddr(subText(record.getRemoteAddr(), 63));
        record.setMethod(subText(record.getMethod(), 15));
        record.setHttpProtocol(subText(record.getHttpProtocol(), 31));
        record.setRequestHost(subText(record.getRequestHost(), 63));
        record.setRequestUri(subText(record.getRequestUri(), 255));
        record.setQueryString(subText(record.getQueryString(), 1023));
        record.setRequestBody(subText(record.getRequestBody(), 4095));
        record.setUserCode(subText(record.getUserCode(), 31));
        record.setUsername(subText(record.getUsername(), 31));
        record.setNickname(subText(record.getNickname(), 31));
        record.setErrorMsg(subText(record.getErrorMsg(), 4095));
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

    private static String getCachedRequestBody(ContentCachingRequestWrapper request) {
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().contains("multipart/form-data")) {
            return null;
        }
        byte[] body = request.getContentAsByteArray();
        if (body.length == 0) {
            return null;
        }
        return new String(body, StandardCharsets.UTF_8);
    }


    private static String getCachedResponseBody(ContentCachingResponseWrapper response) {
        String contentType = response.getContentType();
        if (contentType == null || !contentType.toLowerCase().contains("application/json")) {
            return null;
        }
        byte[] body = response.getContentAsByteArray();
        if (body.length == 0) {
            return null;
        }
        return new String(body, StandardCharsets.UTF_8);
    }


    private static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    private static String extractToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || token.isEmpty()) {
            token = request.getHeader("token");
        }
        return token;
    }

    /**
     * 密码脱敏：将 JSON 中 "assword" 字段的值替换为 ******。
     */
    static String maskPassword(String body) {
        if (body == null || body.isEmpty() || !body.contains("assword")) {
            return body;
        }
        Matcher matcher = PWD_PATTERN.matcher(body);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String value = matcher.group(1);
            String masked = "*".repeat(value.length());
            matcher.appendReplacement(sb, "assword\": \"" + masked + "\"");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Token 脱敏：保留前 8 位 + *** + 后 4 位，短 token 全脱敏。
     */
    static String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return token;
        }
        int len = token.length();
        if (len < 16) {
            return "*".repeat(len);
        }
        return token.substring(0, 8) + "***" + token.substring(len - 4);
    }
}
