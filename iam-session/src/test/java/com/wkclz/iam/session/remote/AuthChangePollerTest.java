package com.wkclz.iam.session.remote;

import com.wkclz.core.exception.SystemException;
import com.wkclz.iam.session.bean.auth.PermissionChangeCommand;
import com.wkclz.iam.session.cache.AuthCacheManager;
import com.wkclz.iam.session.config.IamSessionConfig;
import com.wkclz.iam.session.enums.AuthChangeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthChangePoller 单元测试 — 启用开关/指令分发/异常吞并/调度启停。
 */
class AuthChangePollerTest {

    private RemoteAuthResourceFacade remoteFacade;
    private AuthCacheManager authCacheManager;
    private IamSessionConfig config;
    private AuthChangePoller poller;

    @BeforeEach
    void setUp() {
        remoteFacade = mock(RemoteAuthResourceFacade.class);
        authCacheManager = mock(AuthCacheManager.class);
        config = new IamSessionConfig();
        poller = new AuthChangePoller(remoteFacade, authCacheManager, config);
    }

    @AfterEach
    void tearDown() {
        poller.shutdown();
    }

    private PermissionChangeCommand cmd(AuthChangeType type, String targetKey, long timestamp) {
        PermissionChangeCommand command = new PermissionChangeCommand();
        command.setType(type);
        command.setTargetKey(targetKey);
        command.setTimestamp(timestamp);
        return command;
    }

    @Test
    void intervalNotPositiveSkipsScheduling() {
        config.setAuthChangePollIntervalSeconds(0);
        poller.initPolling();
        assertNull(ReflectionTestUtils.getField(poller, "scheduler"));
    }

    @Test
    void positiveIntervalStartsScheduler() {
        config.setAuthChangePollIntervalSeconds(60);
        poller.initPolling();
        ScheduledExecutorService scheduler =
            (ScheduledExecutorService) ReflectionTestUtils.getField(poller, "scheduler");
        assertNotNull(scheduler);
        // 首轮延迟 = interval，测试期内不应执行 poll
        verify(authCacheManager, times(0)).applyCommand(any());
        // 幂等关闭
        poller.shutdown();
        assertTrue(scheduler.isShutdown());
    }

    @Test
    void pollAppliesEachCommand() {
        when(remoteFacade.fetchChanges()).thenReturn(List.of(
            cmd(AuthChangeType.APP_STATIC, "appA", 100),
            cmd(AuthChangeType.USER, "u1", 101)));

        poller.poll();

        verify(authCacheManager, times(1)).applyCommand(cmd(AuthChangeType.APP_STATIC, "appA", 100));
        verify(authCacheManager, times(1)).applyCommand(cmd(AuthChangeType.USER, "u1", 101));
    }

    @Test
    void pollSwallowsFetchException() {
        when(remoteFacade.fetchChanges()).thenThrow(new RuntimeException("connection refused"));

        assertDoesNotThrow(() -> poller.poll());
        verify(authCacheManager, times(0)).applyCommand(any());
    }

    @Test
    void pollSwallowsFailureCodeSystemException() {
        // R 失败码在门面内部转 SystemException 抛出，轮询吞并不中断调度
        when(remoteFacade.fetchChanges()).thenThrow(SystemException.of("远程取数返回失败: code=500"));

        assertDoesNotThrow(() -> poller.poll());
        verify(authCacheManager, times(0)).applyCommand(any());
    }

    @Test
    void pollSwallowsApplyException() {
        when(remoteFacade.fetchChanges()).thenReturn(List.of(cmd(AuthChangeType.APP_STATIC, "appA", 100)));
        doThrow(new RuntimeException("evict failed")).when(authCacheManager).applyCommand(any());

        assertDoesNotThrow(() -> poller.poll());
    }
}
