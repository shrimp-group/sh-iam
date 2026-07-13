package com.wkclz.auth.filter;

import com.wkclz.auth.bean.Principal;
import com.wkclz.auth.bean.RequestRecord;
import com.wkclz.auth.context.SecurityContext;
import com.wkclz.auth.contract.infra.RequestLogger;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * 请求日志过滤器（最外层，确保任何请求都记录）
 * <p>
 * 职责：采集原始请求/响应数据，填充 RequestRecord。
 * 数据加工（UA 解析、脱敏、截断等）由 RequestLogger 实现方负责。
 */
@Slf4j
@RequiredArgsConstructor
public class RequestRecordFilter extends OncePerRequestFilter {

    private final List<RequestLogger> requestLoggers;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(request, responseWrapper);
        } catch (Exception e) {
            // 异常信息暂存，由 buildRecord 读取
            request.setAttribute("sh.auth.errorMsg", e.getMessage());
            throw e;
        } finally {
            try {
                RequestRecord record = buildRecord(request, responseWrapper, startTime);
                logConsole(record);
                for (RequestLogger logger : requestLoggers) {
                    try {
                        logger.save(record);
                    } catch (Exception ex) {
                        log.error("保存请求日志失败", ex);
                    }
                }
            } catch (Exception ex) {
                log.error("请求日志采集异常", ex);
            } finally {
                SecurityContext.clear();
                responseWrapper.copyBodyToResponse();
            }
        }
    }

    private RequestRecord buildRecord(HttpServletRequest request,
                                       ContentCachingResponseWrapper response,
                                       long startTime) {
        RequestRecord record = new RequestRecord();

        // ─── 基本信息 ───
        record.setMethod(request.getMethod());
        record.setRequestUri(request.getRequestURI());
        record.setQueryString(request.getQueryString());
        record.setHttpProtocol(request.getProtocol());
        record.setCharacterEncoding(request.getCharacterEncoding());
        record.setRequestHost(request.getRemoteHost());
        record.setRemoteAddr(request.getRemoteAddr());

        // ─── 请求头（原始值，加工由 RequestLogger 实现方负责） ───
        record.setUserAgent(request.getHeader("User-Agent"));
        record.setAccept(request.getHeader("Accept"));
        record.setAcceptLanguage(request.getHeader("Accept-Language"));
        record.setAcceptEncoding(request.getHeader("Accept-Encoding"));
        record.setCookie(request.getHeader("Cookie"));
        record.setOrigin(request.getHeader("Origin"));
        record.setReferer(request.getHeader("Referer"));

        // ─── 响应状态、耗时 ───
        record.setHttpStatus(response.getStatus());
        record.setCostTime(System.currentTimeMillis() - startTime);
        record.setRequestTime(LocalDateTime.now());

        // ─── 安全上下文 ───
        record.setToken(SecurityContext.getToken());
        record.setTenantCode(SecurityContext.getTenantCode());
        record.setAppCode(SecurityContext.getAppCode());

        Principal principal = SecurityContext.getPrincipal();
        if (principal != null) {
            record.setUserCode(principal.getUserCode());
            record.setUsername(principal.getUsername());
            record.setNickname(principal.getNickname());
        }

        // ─── 异常信息 ───
        Object errorMsg = request.getAttribute("sh.auth.errorMsg");
        if (errorMsg != null) {
            record.setErrorMsg(errorMsg.toString());
        }

        // ─── 请求体 ───
        if (request instanceof EagerContentCachingRequestWrapper wrapper) {
            byte[] body = wrapper.getCachedBody();
            if (body != null && body.length > 0) {
                record.setRequestBody(new String(body));
            }
        }

        // ─── 响应体 ───
        byte[] respBody = response.getContentAsByteArray();
        if (respBody.length > 0) {
            record.setResponseBody(new String(respBody));
        }

        return record;
    }

    /** 控制台日志（密码脱敏依赖 logback MaskingPatternLayout） */
    private void logConsole(RequestRecord r) {
        int status = r.getHttpStatus() != null ? r.getHttpStatus() : 0;
        String ip = StringUtils.defaultString(r.getRemoteAddr(), "-");
        String user = StringUtils.defaultString(r.getUsername(), "-");
        long cost = r.getCostTime() != null ? r.getCostTime() : 0;
        String method = StringUtils.defaultString(r.getMethod(), "-");
        String uri = StringUtils.defaultString(r.getRequestUri(), "-");
        String body = getLogBody(r);

        if (r.getErrorMsg() != null) {
            log.warn("{} | {} | {} | {}ms | {} | {} | {} | error={}", ip, user, status, cost, method, uri, body, r.getErrorMsg());
        } else {
            log.info("{} | {} | {} | {}ms | {} | {} | {}", ip, user, status, cost, method, uri, body);
        }
    }

    private String getLogBody(RequestRecord r) {
        String body = "GET".equalsIgnoreCase(r.getMethod()) ? r.getQueryString() : r.getRequestBody();
        return StringUtils.isNotBlank(body) ? body : "-";
    }
}
