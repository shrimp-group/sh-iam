package com.wkclz.iam.session.filter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Arrays;
import java.util.List;

/**
 * 默认白名单匹配器 — Ant 风格路径匹配。
 *
 * <p>默认白名单：{@code /** /public/**}（匹配任意层级下的 public 路径）。
 * 可通过 {@code iam.session.white-list} 配置扩展。</p>
 */
@Component
@ConditionalOnMissingBean(WhiteListMatcher.class)
public class AntPathWhiteListMatcher implements WhiteListMatcher {

    public static final String DEFAULT_WHITE_LIST = "/**/public/**";

    private final AntPathMatcher matcher = new AntPathMatcher();
    private final List<String> patterns;

    public AntPathWhiteListMatcher() {
        this.patterns = List.of(DEFAULT_WHITE_LIST);
    }

    public AntPathWhiteListMatcher(String whiteListConfig) {
        if (whiteListConfig == null || whiteListConfig.isBlank()) {
            this.patterns = List.of(DEFAULT_WHITE_LIST);
        } else {
            this.patterns = Arrays.asList(whiteListConfig.split(","));
        }
    }

    @Override
    public boolean isWhiteListed(String requestUri) {
        for (String pattern : patterns) {
            if (matcher.match(pattern.trim(), requestUri)) {
                return true;
            }
        }
        return false;
    }

}
