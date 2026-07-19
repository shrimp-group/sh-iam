package com.wkclz.iam.session.filter;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Token 提取器 SPI — 从 HTTP 请求中提取认证 Token。
 */
public interface TokenResolver {

    /**
     * 从请求中提取 Token，若未找到返回 null。
     */
    String resolve(HttpServletRequest request);

}
