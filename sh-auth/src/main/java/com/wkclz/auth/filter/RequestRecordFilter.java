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

/**
 * 请求日志过滤器（最外层，确保任何请求都记录）
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
        } finally {
            try {
                RequestRecord record = buildRecord(request, responseWrapper, startTime);
                for (RequestLogger logger : requestLoggers) {
                    try {
                        logger.save(record);
                    } catch (Exception e) {
                        log.error("保存请求日志失败", e);
                    }
                }
                SecurityContext.clear();
            } catch (Exception e) {
                log.error("请求日志采集异常", e);
            } finally {
                responseWrapper.copyBodyToResponse();
            }
        }
    }

    private RequestRecord buildRecord(HttpServletRequest request, ContentCachingResponseWrapper response,
                                       long startTime) {
        RequestRecord record = new RequestRecord();
        record.setRequestUri(request.getRequestURI());
        record.setMethod(request.getMethod());
        record.setRemoteAddr(request.getRemoteAddr());
        record.setUserAgent(request.getHeader("User-Agent"));
        record.setHttpStatus(response.getStatus());
        record.setCostTime(System.currentTimeMillis() - startTime);
        record.setRequestTime(LocalDateTime.now());

        Principal principal = SecurityContext.getPrincipal();
        if (principal != null) {
            record.setUserCode(principal.getUserCode());
            record.setUsername(principal.getUsername());
        }

        if (request instanceof EagerContentCachingRequestWrapper wrapper) {
            byte[] body = wrapper.getCachedBody();
            if (body != null && body.length > 0) {
                record.setRequestBody(maskSensitive(new String(body), 4096));
            }
        }

        byte[] respBody = response.getContentAsByteArray();
        if (respBody != null && respBody.length > 0) {
            record.setResponseBody(truncate(new String(respBody), 4096));
        }

        return record;
    }

    private String maskSensitive(String body, int maxLen) {
        String masked = body.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"******\"");
        return truncate(masked, maxLen);
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return null;
        return str.length() > maxLen ? str.substring(0, maxLen) + "...(truncated)" : str;
    }
}
