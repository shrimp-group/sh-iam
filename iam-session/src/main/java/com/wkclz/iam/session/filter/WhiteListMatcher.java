package com.wkclz.iam.session.filter;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.List;

/**
 * 白名单路径匹配器 — 判断请求 URI 是否在白名单中（Ant 风格路径匹配）。
 *
 * <p>默认白名单：{@code /** /public/**}（匹配任意层级下的 public 路径）。
 * 可通过 {@code iam.session.white-list} 配置扩展。</p>
 */
@Component
public class WhiteListMatcher {

    public static final String DEFAULT_WHITE_LIST = "/**/public/**";

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final List<String> patterns;

    public WhiteListMatcher() {
        this.patterns = List.of(DEFAULT_WHITE_LIST);
    }

    public WhiteListMatcher(String whiteListConfig) {
        if (whiteListConfig == null || whiteListConfig.isBlank()) {
            this.patterns = List.of(DEFAULT_WHITE_LIST);
        } else {
            this.patterns = Arrays.asList(whiteListConfig.split(","));
        }
    }

    /**
     * 判断请求 URI 是否为白名单路径。
     *
     * @param requestUri 请求 URI
     * @return true 表示白名单，应放行
     */
    public boolean isWhiteListed(String requestUri) {
        for (String pattern : patterns) {
            if (matcher.match(pattern.trim(), requestUri)) {
                return true;
            }
        }
        return false;
    }
}
