package com.wkclz.iam.session.filter;

import com.wkclz.iam.session.bean.ApiControlConfig;
import com.wkclz.iam.session.cache.ApiControlCache;
import com.wkclz.iam.session.spi.ApiRequestControlProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ApiControlCache 单元测试 — Ant 通配匹配、最长 URI 优先、方法过滤与空白配置跳过。
 */
class ApiControlCacheTest {

    private ApiRequestControlProvider provider;
    private ApiControlCache cache;

    @BeforeEach
    void setUp() {
        provider = mock(ApiRequestControlProvider.class);
        cache = new ApiControlCache(provider);
    }

    private ApiControlConfig api(String method, String uri, String controlJson) {
        ApiControlConfig api = new ApiControlConfig();
        api.setAppCode("app");
        api.setApiCode("api_" + method + "_" + uri.hashCode());
        api.setApiMethod(method);
        api.setApiUri(uri);
        api.setRequestControl(controlJson);
        return api;
    }

    private String json(String enable) {
        return "{\"enable\":" + enable + "}";
    }

    @Test
    void matchExactUri() {
        when(provider.listRequestControlApis())
            .thenReturn(List.of(api("GET", "/api/test", json("true"))));

        ApiControlCache.ApiControlEntry entry = cache.match("GET", "/api/test");
        assertEquals("/api/test", entry.apiUri());
    }

    @Test
    void matchWildcardUri() {
        when(provider.listRequestControlApis())
            .thenReturn(List.of(api("GET", "/api/test/**", json("true"))));

        ApiControlCache.ApiControlEntry entry = cache.match("GET", "/api/test/1");
        assertEquals("/api/test/**", entry.apiUri());
    }

    @Test
    void matchLongestUriWins() {
        when(provider.listRequestControlApis())
            .thenReturn(List.of(
                api("GET", "/api/**", json("true")),
                api("GET", "/api/test/**", json("true"))));

        ApiControlCache.ApiControlEntry entry = cache.match("GET", "/api/test/1");
        assertEquals("/api/test/**", entry.apiUri());
    }

    @Test
    void matchMethodMismatchReturnsNull() {
        when(provider.listRequestControlApis())
            .thenReturn(List.of(api("POST", "/api/test/**", json("true"))));

        assertNull(cache.match("GET", "/api/test/1"));
    }

    @Test
    void matchNoHitReturnsNull() {
        when(provider.listRequestControlApis())
            .thenReturn(List.of(api("GET", "/other/**", json("true"))));

        assertNull(cache.match("GET", "/api/test/1"));
    }

    @Test
    void loadEntriesSkipsBlankRequestControl() {
        when(provider.listRequestControlApis())
            .thenReturn(List.of(
                api("GET", "/blank/**", "   "),
                api("GET", "/valid/**", json("true"))));

        List<ApiControlCache.ApiControlEntry> entries = cache.loadEntries();
        assertEquals(1, entries.size());
        assertEquals("/valid/**", entries.get(0).apiUri());
    }
}
