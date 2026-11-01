package com.wkclz.iam.session.event;

import com.alibaba.fastjson2.JSON;
import com.wkclz.iam.session.bean.auth.PermissionChangeCommand;
import com.wkclz.iam.session.config.IamSessionConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.TimeUnit;

/**
 * 权限变更发布器 — 监听 {@link PermissionChangeEvent}，事务提交后双写 Redis：
 * <ul>
 *   <li>Pub/Sub（channel={@code iam.auth.change.channel}）：同 Redis 实例的各部署实例即时通知
 *       （含发布方自身回环），接收侧经 AuthCacheManager.applyCommand 幂等清理；</li>
 *   <li>变更日志 Hash（key={@link #CHANGE_LOG_KEY}，field={@code type:targetKey}，value=指令 JSON）：
 *       供不同 Redis 的 session 应用经轮询（Task 19 AuthChangePoller）同步；Hash 同键覆盖天然
 *       「只存最后同类指令」，整表 TTL 7 天（每次写入刷新，7 天无变更整表过期 = 轮询方空列表语义）。</li>
 * </ul>
 *
 * <p>发布失败仅 log.error 不阻断业务：本地缓存未受影响，其他实例依赖 URL 级 TTL/远程静态 TTL 硬界兜底。</p>
 *
 * @author shrimp
 */
@Slf4j
@Component
public class PermissionChangePublisher {

    /**
     * 权限变更日志 Hash key（field=type:targetKey，value=指令 JSON，TTL 7 天）
     */
    public static final String CHANGE_LOG_KEY = "iam:auth:change-log";

    private final StringRedisTemplate redisTemplate;
    private final IamSessionConfig config;

    public PermissionChangePublisher(StringRedisTemplate redisTemplate, IamSessionConfig config) {
        this.redisTemplate = redisTemplate;
        this.config = config;
    }

    /**
     * 权限变更处理：事务提交后（无事务上下文时也执行）发布 Pub/Sub + 双写变更日志 Hash。
     *
     * @param event 权限变更事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPermissionChange(PermissionChangeEvent event) {
        log.info("发布权限变更: type={}, key={}, ts={}", event.getType(), event.getTargetKey(), event.getTimestamp());
        try {
            PermissionChangeCommand command = new PermissionChangeCommand();
            command.setType(event.getType());
            command.setTargetKey(event.getTargetKey());
            command.setTimestamp(event.getTimestamp());
            String json = JSON.toJSONString(command);
            // Pub/Sub 即时通知（同 Redis 实例各部署实例）
            redisTemplate.convertAndSend(config.getAuthChangeChannel(), json);
            // 变更日志双写（跨 Redis 轮询源，同键覆盖只存最后同类指令，TTL 刷新）
            redisTemplate.opsForHash().put(CHANGE_LOG_KEY, event.getType() + ":" + event.getTargetKey(), json);
            redisTemplate.expire(CHANGE_LOG_KEY, 7, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("权限变更发布失败（本地缓存未受影响，其他实例依赖 TTL 兜底）: type={}, key={}",
                event.getType(), event.getTargetKey(), e);
        }
    }
}
