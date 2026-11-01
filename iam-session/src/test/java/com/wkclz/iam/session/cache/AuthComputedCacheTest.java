package com.wkclz.iam.session.cache;

import com.google.common.base.Ticker;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AuthComputedCache 单元测试 — URL 级 true/false 缓存、TTL 过期重算、用户级空闲过期与精确清理
 * （可推进 Ticker 注入，纯内存不依赖外部资源）。
 */
class AuthComputedCacheTest {

    /**
     * 可手动推进的 Ticker（纳秒累计）
     */
    private static class MutableTicker extends Ticker {
        private long nanos = 0;

        void advanceSeconds(long seconds) {
            nanos += TimeUnit.SECONDS.toNanos(seconds);
        }

        @Override
        public long read() {
            return nanos;
        }
    }

    private final MutableTicker ticker = new MutableTicker();
    private final AuthComputedCache cache = new AuthComputedCache(60, 300, 100, 100, 10, ticker);

    @Test
    void cachesTrueAndFalsePerUrl() {
        AtomicInteger trueLoads = new AtomicInteger();
        AtomicInteger falseLoads = new AtomicInteger();
        // true 结果：二次 get loader 仅执行一次
        assertEquals(Boolean.TRUE, cache.get("app", "u1", "GET:/a", () -> {
            trueLoads.incrementAndGet();
            return true;
        }));
        assertEquals(Boolean.TRUE, cache.get("app", "u1", "GET:/a", () -> {
            trueLoads.incrementAndGet();
            return true;
        }));
        assertEquals(1, trueLoads.get());
        // false 结果同样被缓存：二次 get 仍返回 false 且 loader 不再执行
        assertEquals(Boolean.FALSE, cache.get("app", "u1", "GET:/b", () -> {
            falseLoads.incrementAndGet();
            return false;
        }));
        assertEquals(Boolean.FALSE, cache.get("app", "u1", "GET:/b", () -> {
            falseLoads.incrementAndGet();
            return false;
        }));
        assertEquals(1, falseLoads.get());
    }

    @Test
    void urlTtlExpiryReloads() {
        AtomicInteger loads = new AtomicInteger();
        cache.get("app", "u1", "GET:/a", () -> {
            loads.incrementAndGet();
            return true;
        });
        // 未到 TTL：命中缓存
        ticker.advanceSeconds(30);
        cache.get("app", "u1", "GET:/a", () -> {
            loads.incrementAndGet();
            return true;
        });
        assertEquals(1, loads.get());
        // 超过 TTL（60s）：重新执行 loader
        ticker.advanceSeconds(31);
        cache.get("app", "u1", "GET:/a", () -> {
            loads.incrementAndGet();
            return true;
        });
        assertEquals(2, loads.get());
    }

    @Test
    void userIdleExpiryInvalidatesWholeUserLayer() {
        // ttl(600) > idle(300)：隔离用户级空闲过期，排除 URL 级 TTL 干扰
        AuthComputedCache idleCache = new AuthComputedCache(600, 300, 100, 100, 10, ticker);
        AtomicInteger loads = new AtomicInteger();
        idleCache.get("app", "u1", "GET:/a", () -> {
            loads.incrementAndGet();
            return true;
        });
        idleCache.get("app", "u1", "GET:/b", () -> {
            loads.incrementAndGet();
            return true;
        });
        assertEquals(2, loads.get());
        // 未超空闲时间：两条 URL 均命中
        ticker.advanceSeconds(100);
        idleCache.get("app", "u1", "GET:/a", () -> {
            loads.incrementAndGet();
            return true;
        });
        idleCache.get("app", "u1", "GET:/b", () -> {
            loads.incrementAndGet();
            return true;
        });
        assertEquals(2, loads.get());
        // 超过用户级空闲时间（最后访问 t=100，再推 301s → 401-100 > 300）：用户层整体失效，全部重算
        ticker.advanceSeconds(301);
        idleCache.get("app", "u1", "GET:/a", () -> {
            loads.incrementAndGet();
            return true;
        });
        idleCache.get("app", "u1", "GET:/b", () -> {
            loads.incrementAndGet();
            return true;
        });
        assertEquals(4, loads.get());
    }

    @Test
    void evictAppIsPrecise() {
        AtomicInteger loadsA = new AtomicInteger();
        AtomicInteger loadsB = new AtomicInteger();
        cache.get("appA", "u1", "GET:/a", () -> {
            loadsA.incrementAndGet();
            return true;
        });
        cache.get("appB", "u1", "GET:/a", () -> {
            loadsB.incrementAndGet();
            return true;
        });
        cache.evictApp("appA");
        // appA 重算，appB 不受影响
        cache.get("appA", "u1", "GET:/a", () -> {
            loadsA.incrementAndGet();
            return true;
        });
        cache.get("appB", "u1", "GET:/a", () -> {
            loadsB.incrementAndGet();
            return true;
        });
        assertEquals(2, loadsA.get());
        assertEquals(1, loadsB.get());
    }

    @Test
    void evictUserIsPrecise() {
        AtomicInteger loadsU1 = new AtomicInteger();
        AtomicInteger loadsU2 = new AtomicInteger();
        cache.get("app", "u1", "GET:/a", () -> {
            loadsU1.incrementAndGet();
            return true;
        });
        cache.get("app", "u2", "GET:/a", () -> {
            loadsU2.incrementAndGet();
            return true;
        });
        cache.evictUser("u1", "SESSION_END");
        cache.get("app", "u1", "GET:/a", () -> {
            loadsU1.incrementAndGet();
            return true;
        });
        cache.get("app", "u2", "GET:/a", () -> {
            loadsU2.incrementAndGet();
            return true;
        });
        assertTrue(loadsU1.get() >= 2);
        assertEquals(1, loadsU2.get());
        assertFalse(cache.get("app", "u2", "GET:/b", () -> false));
    }
}
