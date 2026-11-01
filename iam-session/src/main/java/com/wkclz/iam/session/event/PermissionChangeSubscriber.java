package com.wkclz.iam.session.event;

import com.alibaba.fastjson2.JSON;
import com.wkclz.iam.session.bean.auth.PermissionChangeCommand;
import com.wkclz.iam.session.cache.AuthCacheManager;
import com.wkclz.iam.session.config.IamSessionConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 权限变更订阅器 — 订阅 Redis Pub/Sub 权限变更 channel（同 Redis 全部部署实例，含发布方自身回环，
 * 单一代码路径），解析 {@link PermissionChangeCommand} JSON 后委托
 * {@link AuthCacheManager#applyCommand}（时间戳去重在 Manager 内完成）做本地缓存精确清理。
 *
 * <p>channel 与发布端（PermissionChangePublisher）一致，取 {@code iam.auth.change.channel}。
 * RedisMessageListenerContainer 由 sh-redis RedisConfig 注册；容器缺失时（极简测试上下文等）
 * 优雅降级跳过订阅，跨实例通知依赖 TTL 硬界兜底。</p>
 *
 * @author shrimp
 */
@Slf4j
@Component
public class PermissionChangeSubscriber implements MessageListener {

    @Resource
    private IamSessionConfig config;
    @Resource
    private AuthCacheManager authCacheManager;
    @Resource
    private ObjectProvider<RedisMessageListenerContainer> containerProvider;

    /**
     * @PostConstruct 时从 ObjectProvider 解析；容器缺失保持 null（跳过订阅）
     */
    private RedisMessageListenerContainer container;

    /**
     * 注册订阅（应用启动时完成，不阻塞启动失败路径；容器缺失时降级跳过）。
     */
    @PostConstruct
    public void subscribe() {
        container = containerProvider.getIfAvailable();
        if (container == null) {
            log.warn("未找到 RedisMessageListenerContainer，权限变更订阅未注册（跨实例通知不可用，依赖 TTL 兜底）");
            return;
        }
        String channel = config.getAuthChangeChannel();
        container.addMessageListener(this, new ChannelTopic(channel));
        log.info("权限变更订阅已注册: channel={}", channel);
    }

    /**
     * 消费权限变更消息：解析 JSON → 委托 applyCommand；异常仅记日志，不中断订阅线程。
     *
     * @param message 消息体（body 为指令 JSON）
     * @param pattern 订阅模式（channel 订阅时为 null，忽略）
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            PermissionChangeCommand command = JSON.parseObject(json, PermissionChangeCommand.class);
            if (command == null || command.getType() == null || command.getTargetKey() == null) {
                log.warn("权限变更消息非法，忽略: {}", json);
                return;
            }
            authCacheManager.applyCommand(command);
        } catch (Exception e) {
            log.error("权限变更消息处理异常（不影响订阅线程）: message={}",
                message == null ? null : new String(message.getBody(), StandardCharsets.UTF_8), e);
        }
    }

    /**
     * 销毁时移除订阅（容器随应用销毁，此处保持整洁）。
     */
    @PreDestroy
    public void unsubscribe() {
        if (container != null) {
            container.removeMessageListener(this);
            log.info("权限变更订阅已移除");
        }
    }
}
