package com.wkclz.iam.session.event;

import com.wkclz.iam.session.config.IamSessionConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PermissionChangePublisher 单元测试 — Pub/Sub channel 发布、变更日志 Hash 双写与发布异常吞并。
 */
@ExtendWith(MockitoExtension.class)
class PermissionChangePublisherTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private IamSessionConfig config;

    private PermissionChangePublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new PermissionChangePublisher(redisTemplate, config);
    }

    @Test
    void publishesToConfiguredChannel() {
        when(config.getAuthChangeChannel()).thenReturn("iam:auth:change");
        publisher.onPermissionChange(PermissionChangeEvent.appChanged("appA"));
        // channel 使用配置值，JSON 载荷含类型与目标键
        verify(redisTemplate).convertAndSend(eq("iam:auth:change"), contains("\"APP_STATIC\""));
        verify(redisTemplate).convertAndSend(anyString(), contains("\"targetKey\":\"appA\""));
    }

    @Test
    void dualWritesChangeLogHashWithTtl() {
        when(config.getAuthChangeChannel()).thenReturn("iam:auth:change");
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        publisher.onPermissionChange(PermissionChangeEvent.userChanged("user_1"));
        // Hash 双写：field=type:targetKey，value=指令 JSON
        verify(hashOperations).put(eq(PermissionChangePublisher.CHANGE_LOG_KEY),
            eq("USER:user_1"), contains("\"targetKey\":\"user_1\""));
        // 整表 TTL 7 天刷新
        verify(redisTemplate).expire(PermissionChangePublisher.CHANGE_LOG_KEY, 7, TimeUnit.DAYS);
    }

    @Test
    void publishErrorSwallowed() {
        when(config.getAuthChangeChannel()).thenReturn("iam:auth:change");
        doThrow(new RuntimeException("redis down"))
            .when(redisTemplate).convertAndSend(anyString(), anyString());
        // 发布失败不向外抛（不阻断业务主流程）
        assertDoesNotThrow(() -> publisher.onPermissionChange(PermissionChangeEvent.appChanged("appA")));
    }
}
