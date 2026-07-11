package com.wkclz.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 请求体可重复读取包装过滤器（最高优先级）
 */
@Slf4j
public class RequestWrapperFilter extends OncePerRequestFilter implements Ordered {

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        EagerContentCachingRequestWrapper wrappedRequest = new EagerContentCachingRequestWrapper(request);
        filterChain.doFilter(wrappedRequest, response);
    }
}
