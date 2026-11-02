package com.wkclz.iam.session.cache;

import com.google.common.base.Ticker;
import com.wkclz.iam.session.bean.auth.AppAuthResource;
import com.wkclz.iam.session.bean.auth.PermissionChangeCommand;
import com.wkclz.iam.session.config.IamSessionConfig;
import com.wkclz.iam.session.enums.AuthChangeType;
import com.wkclz.iam.session.spi.AuthResourceFacade;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * AuthCacheManager 单元测试 — 按需加载合并、指令联动清理、时间戳去重与请求控制最长匹配
 * （fake facade 计数，纯内存不依赖外部资源）。
 */
class AuthCacheManagerTest {

    /**
     * 计数 fake facade：可注入请求控制配置
     */
    private static class CountingFacade implements AuthResourceFacade {

        final AtomicInteger appLoads = new AtomicInteger();
        final AtomicInteger userLoads = new AtomicInteger();
        List<AppAuthResource.ApiControlEntry> controls = List.of();
        String controlsAppCode = null;

        @Override
        public AppAuthResource loadAppResource(String appCode) {
            appLoads.incrementAndGet();
            AppAuthResource resource = AppAuthResource.empty(appCode);
            if (appCode.equals(controlsAppCode)) {
                resource.getRequestControls().addAll(controls);
            }
            return resource;
        }

        @Override
        public List<String> loadUserRoleCodes(String userCode) {
            userLoads.incrementAndGet();
            return List.of("R1", "R2");
        }
    }

    private final CountingFacade facade = new CountingFacade();
    private final AuthComputedCache computedCache =
        new AuthComputedCache(300, 1800, 100, 100, 10, Ticker.systemTicker());
    private final AuthCacheManager manager = new AuthCacheManager(facade, computedCache, config(), noRemote());

    @SuppressWarnings("unchecked")
    private static org.springframework.beans.factory.ObjectProvider<com.wkclz.iam.session.remote.RemoteClientConfig> noRemote() {
        // getIfAvailable() 未打桩默认返回 null（本地形态）
        return org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
    }

    /**
     * 可推进时间源（验证远程形态 L1 TTL）
     */
    private static class MutableTicker extends Ticker {
        private final java.util.concurrent.atomic.AtomicLong nanos = new java.util.concurrent.atomic.AtomicLong();

        void advanceSeconds(long seconds) {
            nanos.addAndGet(java.util.concurrent.TimeUnit.SECONDS.toNanos(seconds));
        }

        @Override
        public long read() {
            return nanos.get();
        }
    }

    private IamSessionConfig config() {
        IamSessionConfig config = new IamSessionConfig();
        config.setAuthCacheStaticMaxApps(10);
        config.setAuthCacheUserMaxUsers(100);
        config.setAuthCacheUserIdleSeconds(1800);
        config.setAuthCacheComputedTtlSeconds(300);
        config.setAuthCacheComputedMaxUrls(100);
        return config;
    }

    private PermissionChangeCommand cmd(AuthChangeType type, String targetKey, long timestamp) {
        PermissionChangeCommand command = new PermissionChangeCommand();
        command.setType(type);
        command.setTargetKey(targetKey);
        command.setTimestamp(timestamp);
        return command;
    }

    @Test
    void loadsAppResourceOnceForRepeatedAccess() {
        for (int i = 0; i < 10; i++) {
            manager.getAppResource("appA");
        }
        assertEquals(1, facade.appLoads.get());
    }

    @Test
    void appStaticCommandInvalidatesL1AndL3() {
        manager.getAppResource("appA");
        AtomicInteger compute = new AtomicInteger();
        manager.hasUrlPermission("appA", "u1", "GET:/x", () -> {
            compute.incrementAndGet();
            return true;
        });
        assertEquals(1, compute.get());
        manager.applyCommand(cmd(AuthChangeType.APP_STATIC, "appA", 100));
        // L1 失效：重新加载
        manager.getAppResource("appA");
        assertEquals(2, facade.appLoads.get());
        // L3 联动清理：重新计算
        manager.hasUrlPermission("appA", "u1", "GET:/x", () -> {
            compute.incrementAndGet();
            return true;
        });
        assertEquals(2, compute.get());
    }

    @Test
    void userCommandInvalidatesL2AndL3OnlyForTargetUser() {
        manager.getUserRoleCodes("u1");
        manager.getUserRoleCodes("u2");
        assertEquals(2, facade.userLoads.get());
        AtomicInteger computeU1 = new AtomicInteger();
        AtomicInteger computeU2 = new AtomicInteger();
        manager.hasUrlPermission("appA", "u1", "GET:/x", () -> {
            computeU1.incrementAndGet();
            return true;
        });
        manager.hasUrlPermission("appA", "u2", "GET:/x", () -> {
            computeU2.incrementAndGet();
            return true;
        });
        manager.applyCommand(cmd(AuthChangeType.USER, "u1", 100));
        // L2：u1 重新加载，u2 命中缓存
        manager.getUserRoleCodes("u1");
        manager.getUserRoleCodes("u2");
        assertEquals(3, facade.userLoads.get());
        // L3：u1 重算，u2 仍缓存（loader 不执行）
        manager.hasUrlPermission("appA", "u1", "GET:/x", () -> {
            computeU1.incrementAndGet();
            return true;
        });
        manager.hasUrlPermission("appA", "u2", "GET:/x", () -> {
            computeU2.incrementAndGet();
            return true;
        });
        assertEquals(2, computeU1.get());
        assertEquals(1, computeU2.get());
    }

    @Test
    void staleCommandIgnoredByTimestampDedup() {
        manager.getAppResource("appA");
        manager.applyCommand(cmd(AuthChangeType.APP_STATIC, "appA", 200));
        manager.getAppResource("appA");
        assertEquals(2, facade.appLoads.get());
        // 同 key 旧时间戳指令：忽略，缓存未再次失效
        manager.applyCommand(cmd(AuthChangeType.APP_STATIC, "appA", 100));
        manager.getAppResource("appA");
        assertEquals(2, facade.appLoads.get());
    }

    @Test
    void matchRequestControlLongestMatchWins() {
        facade.controlsAppCode = "appA";
        facade.controls = List.of(
            new AppAuthResource.ApiControlEntry("api_1", "GET", "/api/**", null),
            new AppAuthResource.ApiControlEntry("api_2", "GET", "/api/test/**", null));
        AppAuthResource.ApiControlEntry matched = manager.matchRequestControl("appA", "GET", "/api/test/1");
        assertEquals("/api/test/**", matched.apiUri());
    }

    @Test
    void matchRequestControlRequestMethodWildcard() {
        facade.controlsAppCode = "appA";
        facade.controls = List.of(
            new AppAuthResource.ApiControlEntry("api_x", "REQUEST", "/any/**", null));
        AppAuthResource.ApiControlEntry matched = manager.matchRequestControl("appA", "delete", "/any/1");
        assertEquals("/any/**", matched.apiUri());
    }

    @Test
    void matchRequestControlAppIsolation() {
        facade.controlsAppCode = "appA";
        facade.controls = List.of(
            new AppAuthResource.ApiControlEntry("api_1", "GET", "/api/**", null));
        // appA 有配置，appB 未配置
        assertNull(manager.matchRequestControl("appB", "GET", "/api/1"));
    }

    private org.springframework.beans.factory.ObjectProvider<com.wkclz.iam.session.remote.RemoteClientConfig> remoteProvider(
        com.wkclz.iam.session.remote.RemoteClientConfig remote) {
        org.springframework.beans.factory.ObjectProvider<com.wkclz.iam.session.remote.RemoteClientConfig> provider =
            org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
        org.mockito.Mockito.when(provider.getIfAvailable()).thenReturn(remote);
        return provider;
    }

    @Test
    void remoteFormL1ExpiresAfterTtl() {
        com.wkclz.iam.session.remote.RemoteClientConfig remote = new com.wkclz.iam.session.remote.RemoteClientConfig();
        remote.setServerUrl("http://sso.example.com");
        IamSessionConfig config = config();
        config.setAuthCacheStaticRemoteTtlSeconds(300);
        MutableTicker ticker = new MutableTicker();
        AuthCacheManager remoteManager = new AuthCacheManager(facade, computedCache, config, remoteProvider(remote), ticker);

        remoteManager.getAppResource("appA");
        assertEquals(1, facade.appLoads.get());
        ticker.advanceSeconds(299);
        remoteManager.getAppResource("appA");
        assertEquals(1, facade.appLoads.get());
        // 累计 301s > TTL 300s：过期重载（静态陈旧性硬界）
        ticker.advanceSeconds(2);
        remoteManager.getAppResource("appA");
        assertEquals(2, facade.appLoads.get());
    }

    @Test
    void localFormL1HasNoTtl() {
        MutableTicker ticker = new MutableTicker();
        AuthCacheManager localManager = new AuthCacheManager(facade, computedCache, config(), noRemote(), ticker);

        localManager.getAppResource("appA");
        assertEquals(1, facade.appLoads.get());
        // 本地形态无 TTL：时间远超远程 TTL 后仍命中缓存
        ticker.advanceSeconds(100_000);
        localManager.getAppResource("appA");
        assertEquals(1, facade.appLoads.get());
    }
}
