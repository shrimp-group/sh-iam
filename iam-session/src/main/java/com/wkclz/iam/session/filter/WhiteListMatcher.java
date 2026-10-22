package com.wkclz.iam.session.filter;

import com.wkclz.iam.session.config.IamSessionConfig;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    public static List<String> DEFAULT_WHITE_LIST = List.of("/**/public/**");

    private final AntPathMatcher matcher = new AntPathMatcher();
    private List<String> patterns;

    @Autowired
    private IamSessionConfig iamSessionConfig;

    /**
     * 判断请求 URI 是否为白名单路径。
     *
     * @param requestUri 请求 URI
     * @return true 表示白名单，应放行
     */
    public boolean isWhiteListed(String requestUri) {
        init();
        for (String pattern : patterns) {
            if (matcher.match(pattern.trim(), requestUri)) {
                return true;
            }
        }
        return false;
    }

    private void init() {
        if (patterns != null) {
            return;
        }
        String whiteList = iamSessionConfig.getWhiteList();
        if (StringUtils.isBlank(whiteList)) {
            patterns = DEFAULT_WHITE_LIST;
            return;
        }
        DEFAULT_WHITE_LIST.addAll(Arrays.asList(whiteList.split(",")));
        patterns = DEFAULT_WHITE_LIST;
    }

}
