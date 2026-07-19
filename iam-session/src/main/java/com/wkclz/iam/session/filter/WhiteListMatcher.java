package com.wkclz.iam.session.filter;

/**
 * 白名单路径匹配器 SPI — 判断请求 URI 是否在白名单中。
 */
public interface WhiteListMatcher {

    /**
     * 判断请求 URI 是否为白名单路径。
     *
     * @param requestUri 请求 URI
     * @return true 表示白名单，应放行
     */
    boolean isWhiteListed(String requestUri);

}
