package com.wkclz.iam.session.cache;

import com.alibaba.fastjson2.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wkclz.iam.session.bean.ApiControlConfig;
import com.wkclz.iam.session.bean.ApiRequestControl;
import com.wkclz.iam.session.spi.ApiRequestControlProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 请求控制 API 配置缓存
 * <p>
 * 通过 {@link ApiRequestControlProvider} SPI 加载所有已配置请求控制（request_control 非空）的 API，
 * 缓存于 Guava Cache（最大 1000 条、5 分钟过期），提供基于 HTTP 方法与 URI 的最长匹配查询（AntPathMatcher）。
 *
 * @author shrimp
 */
@Component
public class ApiControlCache {

    private static final Logger log = LoggerFactory.getLogger(ApiControlCache.class);

    /**
     * Guava Cache 固定键（整份配置列表）
     */
    private static final String CACHE_KEY = "request-control-apis";

    /**
     * 请求控制 API 配置条目
     *
     * @param appCode        应用编码
     * @param apiCode        API 编码
     * @param apiMethod      HTTP 方法
     * @param apiUri         API URI（支持 Ant 通配）
     * @param requestControl 请求控制配置
     */
    public record ApiControlEntry(String appCode, String apiCode, String apiMethod, String apiUri,
                                  ApiRequestControl requestControl) {
    }

    private final ApiRequestControlProvider provider;

    /**
     * 请求控制 API 配置缓存（maximumSize 1000、expireAfterWrite 5 分钟）
     */
    private final Cache<String, List<ApiControlEntry>> cache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public ApiControlCache(ApiRequestControlProvider provider) {
        this.provider = provider;
    }

    /**
     * 从 SPI 提供方加载所有已配置请求控制的 API（供 Guava Cache 回调）
     *
     * @return 请求控制 API 配置条目列表
     */
    public List<ApiControlEntry> loadEntries() {
        log.info("加载请求控制 API 配置缓存");
        List<ApiControlConfig> apis = provider.listRequestControlApis();
        List<ApiControlEntry> entries = new ArrayList<>();
        if (apis != null) {
            for (ApiControlConfig api : apis) {
                String requestControl = api.getRequestControl();
                // 配置为空白则跳过（isBlank 已覆盖 null 与纯空白，避免 NPE）
                if (StringUtils.isBlank(requestControl)) {
                    continue;
                }
                try {
                    ApiRequestControl control = JSON.parseObject(requestControl, ApiRequestControl.class);
                    entries.add(new ApiControlEntry(api.getAppCode(), api.getApiCode(), api.getApiMethod(), api.getApiUri(), control));
                } catch (Exception e) {
                    log.warn("请求控制配置解析失败，跳过: apiCode={}, error={}", api.getApiCode(), e.getMessage());
                }
            }
        }
        log.info("加载请求控制 API 配置缓存完成: 共 {} 条", entries.size());
        return entries;
    }

    /**
     * 匹配请求对应的请求控制配置
     * <p>
     * 遍历全部条目，取 HTTP 方法忽略大小写相等且 URI（Ant 通配）匹配、apiUri 最长（最具体）者；
     * 无命中返回 null。
     *
     * @param method HTTP 方法
     * @param uri    请求 URI
     * @return 匹配到的配置条目；无命中返回 null
     */
    public ApiControlEntry match(String method, String uri) {
        List<ApiControlEntry> entries;
        try {
            entries = cache.get(CACHE_KEY, this::loadEntries);
        } catch (ExecutionException e) {
            log.error("获取请求控制配置缓存异常: error={}", e.getMessage(), e);
            return null;
        }
        ApiControlEntry matched = null;
        for (ApiControlEntry entry : entries) {
            if (entry.apiMethod() != null && entry.apiMethod().equalsIgnoreCase(method)
                && entry.apiUri() != null && pathMatcher.match(entry.apiUri(), uri)) {
                // 取 apiUri 最长（最具体）者
                if (matched == null || entry.apiUri().length() > matched.apiUri().length()) {
                    matched = entry;
                }
            }
        }
        return matched;
    }
}
