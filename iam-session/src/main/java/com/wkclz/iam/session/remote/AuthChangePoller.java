package com.wkclz.iam.session.remote;

import com.wkclz.iam.session.bean.auth.PermissionChangeCommand;
import com.wkclz.iam.session.cache.AuthCacheManager;
import com.wkclz.iam.session.config.IamSessionConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 权限变更轮询器 — 跨 Redis 场景变更同步主通道（宿主应用与 portal 不同 Redis 套餐时 Pub/Sub 不可达）。
 *
 * <p>三层防线之一：① 轮询同步（本类，主通道）；② L3 URL 级 TTL（判断偏差硬界）；
 * ③ 远程形态 L1 TTL（{@code AuthCacheManager} 静态陈旧性硬界）。</p>
 *
 * <p>条件装配：仅当 {@code iam.session.remote.server-url} 配置时注册（与远程门面联动）；
 * 自持单线程调度（不依赖宿主 @EnableScheduling），仅当 {@code iam.auth.change.poll-interval-seconds > 0} 时启动；
 * {@code scheduleWithFixedDelay} 保证上一轮完成后才计时，慢响应不会堆积。</p>
 *
 * <p>拉取与订阅共用 {@code AuthCacheManager.applyCommand} 去重入口，同 Redis 场景两者并存无副作用。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "iam.session.remote.server-url")
public class AuthChangePoller {

    private final RemoteAuthResourceFacade remoteFacade;
    private final AuthCacheManager authCacheManager;
    private final IamSessionConfig config;

    /**
     * 自持调度线程池（未启用轮询时为 null）
     */
    private volatile ScheduledExecutorService scheduler;

    public AuthChangePoller(RemoteAuthResourceFacade remoteFacade, AuthCacheManager authCacheManager,
                            IamSessionConfig config) {
        this.remoteFacade = remoteFacade;
        this.authCacheManager = authCacheManager;
        this.config = config;
    }

    /**
     * 启动轮询（poll-interval-seconds <= 0 时跳过）
     */
    @PostConstruct
    public void initPolling() {
        Integer interval = config.getAuthChangePollIntervalSeconds();
        if (interval == null || interval <= 0) {
            log.info("权限变更轮询未启用: iam.auth.change.poll-interval-seconds={}", interval);
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "iam-auth-change-poller");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(this::poll, interval, interval, TimeUnit.SECONDS);
        log.info("权限变更轮询已启动: interval={}s", interval);
    }

    /**
     * 停止轮询
     */
    @PreDestroy
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            log.info("权限变更轮询已停止");
        }
    }

    /**
     * 单轮轮询：拉取变更指令（每键最新）→ 逐条 applyCommand（时间戳去重使重复拉取为 no-op）。
     * 全 try-catch，失败 log.error 等待下轮，不中断调度。
     */
    void poll() {
        try {
            List<PermissionChangeCommand> commands = remoteFacade.fetchChanges();
            for (PermissionChangeCommand command : commands) {
                authCacheManager.applyCommand(command);
            }
            log.debug("权限变更轮询完成: commands={}", commands.size());
        } catch (Exception e) {
            log.error("权限变更轮询失败，等待下轮重试: error={}", e.getMessage(), e);
        }
    }

}
