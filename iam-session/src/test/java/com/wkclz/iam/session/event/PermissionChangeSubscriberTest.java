package com.wkclz.iam.session.event;

import com.alibaba.fastjson2.JSON;
import com.wkclz.iam.session.bean.auth.PermissionChangeCommand;
import com.wkclz.iam.session.cache.AuthCacheManager;
import com.wkclz.iam.session.config.IamSessionConfig;
import com.wkclz.iam.session.enums.AuthChangeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * PermissionChangeSubscriber 单元测试 — 订阅注册、消息解析委托 applyCommand 与异常吞并。
 */
@ExtendWith(MockitoExtension.class)
class PermissionChangeSubscriberTest {

    @Mock
    private ObjectProvider<RedisMessageListenerContainer> containerProvider;
    @Mock
    private RedisMessageListenerContainer container;
    @Mock
    private AuthCacheManager authCacheManager;
    @Mock
    private IamSessionConfig config;

    private PermissionChangeSubscriber subscriber;

    @BeforeEach
    void setUp() {
        // 订阅器为 @Resource 字段注入，测试经反射装配（对齐主代码注入风格）
        subscriber = new PermissionChangeSubscriber();
        ReflectionTestUtils.setField(subscriber, "containerProvider", containerProvider);
        ReflectionTestUtils.setField(subscriber, "authCacheManager", authCacheManager);
        ReflectionTestUtils.setField(subscriber, "config", config);
    }

    private Message messageOf(String json) {
        // Message 为接口，测试用匿名实现模拟 Redis 字节消息
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        return new Message() {
            @Override
            public byte[] getBody() {
                return body;
            }

            @Override
            public byte[] getChannel() {
                return "iam:auth:change".getBytes(StandardCharsets.UTF_8);
            }
        };
    }

    @Test
    void subscribesOnStartupWithConfiguredChannel() {
        when(containerProvider.getIfAvailable()).thenReturn(container);
        when(config.getAuthChangeChannel()).thenReturn("iam:auth:change");
        subscriber.subscribe();
        ArgumentCaptor<ChannelTopic> topicCaptor = ArgumentCaptor.forClass(ChannelTopic.class);
        verify(container).addMessageListener(eq(subscriber), topicCaptor.capture());
        assertEquals("iam:auth:change", topicCaptor.getValue().getTopic());
    }

    @Test
    void skipsSubscriptionWhenContainerAbsent() {
        when(containerProvider.getIfAvailable()).thenReturn(null);
        assertDoesNotThrow(subscriber::subscribe);
        verifyNoInteractions(config);
    }

    @Test
    void delegatesValidCommandToApplyCommand() {
        PermissionChangeCommand command = new PermissionChangeCommand();
        command.setType(AuthChangeType.APP_STATIC);
        command.setTargetKey("appA");
        command.setTimestamp(1234567890L);
        subscriber.onMessage(messageOf(JSON.toJSONString(command)), null);
        ArgumentCaptor<PermissionChangeCommand> captor = ArgumentCaptor.forClass(PermissionChangeCommand.class);
        verify(authCacheManager).applyCommand(captor.capture());
        assertEquals(AuthChangeType.APP_STATIC, captor.getValue().getType());
        assertEquals("appA", captor.getValue().getTargetKey());
        assertEquals(1234567890L, captor.getValue().getTimestamp());
    }

    @Test
    void ignoresInvalidJsonOrMissingFields() {
        // 非法 JSON
        assertDoesNotThrow(() -> subscriber.onMessage(messageOf("not-json{"), null));
        // 缺失字段（type 为 null）
        assertDoesNotThrow(() -> subscriber.onMessage(messageOf("{\"targetKey\":\"appA\"}"), null));
        verifyNoInteractions(authCacheManager);
    }

    @Test
    void processingErrorDoesNotBreakSubscription() {
        PermissionChangeCommand command = new PermissionChangeCommand();
        command.setType(AuthChangeType.APP_STATIC);
        command.setTargetKey("appA");
        command.setTimestamp(1L);
        doThrow(new RuntimeException("apply failed"))
            .when(authCacheManager).applyCommand(any(PermissionChangeCommand.class));
        // 处理异常被吞，不向订阅线程外抛
        assertDoesNotThrow(() -> subscriber.onMessage(messageOf(JSON.toJSONString(command)), null));
    }
}
