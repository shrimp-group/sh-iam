package com.wkclz.iam.session.filter;

import com.wkclz.core.identity.IdentityContext;
import com.wkclz.iam.session.bean.RequestRecord;
import com.wkclz.iam.session.spi.RequestRecordHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 请求日志采集过滤器 — 在请求执行前后采集请求/响应信息，异步写入请求日志。
 *
 * <p>执行顺序：
 * <ol>
 *   <li>记录开始时间，包装 request/response</li>
 *   <li>调用下层过滤器链（{@link SessionAuthFilter} → 业务 Controller）</li>
 *   <li>请求结束后采集响应信息、用户身份、计算耗时</li>
 *   <li>密码脱敏后通过 {@link RequestRecordHandler} SPI 异步持久化</li>
 * </ol>
 *
 * <p>Order 优先级高于 {@link SessionAuthFilter}（无显式 Order），作为最外层包装过滤器。</p>
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class RequestRecordFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestRecordFilter.class);

    private static final Pattern PWD_PATTERN = Pattern.compile("assword\"\\s*:\\s*\"(.*?)\"");

    /**
     * 不记录日志的 URI 模式
     */
    private static final String[] NO_LOG_URIS = {"/public/status", "/public/health"};

    @Autowired
    private RequestRecordHandler requestRecordHandler;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // 包装 request（缓存请求体）和 response（缓存响应体）
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 0);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } catch (Exception e) {
            // 捕获异常，不影响上层异常处理
            log.debug("Request processing error: {}", e.getMessage());
            throw e;
        } finally {
            // 收集日志信息（在 SessionAuthFilter 的 finally 已清理 IdentityContext 后执行）
            RequestRecord logData = collectLogData(wrappedRequest, wrappedResponse, startTime);
            // 必须执行，否则客户端收不到响应体
            wrappedResponse.copyBodyToResponse();
            // 异步持久化
            persistAsync(logData);
        }
    }

    /**
     * 收集请求/响应日志数据。
     * <p>用户信息从请求属性获取——由 {@link SessionAuthFilter} 在设置完 IdentityContext 后
     * 存入请求属性，然后在 finally 中清理 IdentityContext。</p>
     */
    private RequestRecord collectLogData(ContentCachingRequestWrapper request,
                                         ContentCachingResponseWrapper response,
                                         long startTime) {
        RequestRecord record = new RequestRecord();

        // 基础请求信息
        record.setMethod(request.getMethod());
        record.setRequestUri(request.getRequestURI());
        record.setQueryString(request.getQueryString());
        record.setHttpProtocol(request.getProtocol());
        record.setCharacterEncoding(request.getCharacterEncoding());

        // 请求体（JSON 内容）
        String requestBody = getCachedBody(request.getContentAsByteArray());
        record.setRequestBody(maskPassword(requestBody));

        // 请求头
        record.setUserAgent(request.getHeader("User-Agent"));
        record.setAccept(request.getHeader("Accept"));
        record.setAcceptLanguage(request.getHeader("Accept-Language"));
        record.setAcceptEncoding(request.getHeader("Accept-Encoding"));
        record.setCookie(request.getHeader("Cookie"));
        record.setOrigin(request.getHeader("Origin"));
        record.setReferer(request.getHeader("Referer"));

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
        String responseBody = getCachedBody(response.getContentAsByteArray());
        record.setResponseBody(maskPassword(responseBody));

        // 耗时
        record.setCostTime(System.currentTimeMillis() - startTime);

        return record;
    }

    /**
     * 异步持久化请求日志。
     */
    private void persistAsync(RequestRecord record) {
        String uri = record.getRequestUri();
        if (uri != null && isNoLogUri(uri)) {
            return;
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

    private static String getCachedBody(byte[] body) {
        if (body == null || body.length == 0) {
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

    private static boolean isNoLogUri(String uri) {
        for (String pattern : NO_LOG_URIS) {
            if (uri.contains(pattern) || uri.matches(pattern.replace("*", ".*"))) {
                return true;
            }
        }
        return false;
    }
}
