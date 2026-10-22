package com.wkclz.iam.session.filter;

import com.wkclz.iam.session.config.IamSessionConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 白名单路径匹配器 — 判断请求 URI 是否在白名单中（Ant 风格路径匹配）。
 *
 * <p>默认白名单：{@code /** /public/**}（匹配任意层级下的 public 路径）。
 * 可通过 {@code iam.session.white-list} 配置扩展（逗号分隔，追加到默认白名单之后）。
 * 白名单按需加载：首次匹配时初始化一次，线程安全。</p>
 */
@Component
public class WhiteListMatcher {

    private static final Logger log = LoggerFactory.getLogger(WhiteListMatcher.class);

    private static final List<String> DEFAULT_WHITE_LIST = List.of("/**/public/**");

    private final AntPathMatcher matcher = new AntPathMatcher();

    private volatile List<String> patterns;

    @Autowired
    private IamSessionConfig iamSessionConfig;

    /**
     * 判断请求 URI 是否为白名单路径。
     *
     * @param requestUri 请求 URI
     * @return true 表示白名单，应放行
     */
    public boolean isWhiteListed(String requestUri) {
        List<String> current = patterns;
        if (current == null) {
            current = init();
        }
        for (String pattern : current) {
            if (matcher.match(pattern, requestUri)) {
                return true;
            }
        }
        return false;
    }

    private synchronized List<String> init() {
        if (patterns != null) {
            return patterns;
        }
        List<String> configured = new ArrayList<>(DEFAULT_WHITE_LIST);
        String whiteList = iamSessionConfig.getWhiteList();
        if (StringUtils.isNotBlank(whiteList)) {
            Arrays.stream(whiteList.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .forEach(configured::add);
        }
        patterns = List.copyOf(configured);
        log.info("WhiteListMatcher patterns: {}", patterns);
        return patterns;
    }
}
