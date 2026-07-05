package com.wkclz.iam.sso.schedule;


import com.wkclz.iam.common.entity.IamRequestLog;
import com.wkclz.iam.common.helper.IpLocalCacheHelper;
import com.wkclz.iam.sso.mapper.SsoRequestLogMapper;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author shrimp
 */
@Slf4j
@Component
public class Ip2LocationScheduler implements ApplicationRunner {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ip2addr");
        t.setDaemon(false);
        return t;
    });

    private final AtomicBoolean running = new AtomicBoolean(true);

    @Resource
    private SsoRequestLogMapper ssoRequestLogMapper;

    // 消化队列
    @Override
    public void run(ApplicationArguments args) {
        log.info("Ip2LocationScheduler 启动 IP 归属地异步消费任务");
        executor.submit(this::consumeQueue);
    }

    /**
     * TD-016: 消费循环抽离为独立方法，便于异常处理与可读性
     */
    private void consumeQueue() {
        while (running.get()) {
            try {
                String poll = IpLocalCacheHelper.pollQueue();
                if (poll == null) {
                    Thread.sleep(120);
                    continue;
                }
                IamRequestLog location = IpLocalCacheHelper.getLocation(poll);

                if (location == null) {
                    continue;
                }

                location.setCreateTime(LocalDateTime.now().plusMonths(-1));
                Integer count = ssoRequestLogMapper.updateMostLocation(location);
                log.info("remoteAddr 2 location --->, ip: {}, count : {} ", location.getRemoteAddr(), count);

                // 更新 DB
                Thread.sleep(1200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Ip2LocationScheduler 消费线程被中断，退出循环");
                break;
            } catch (Exception e) {
                log.error("Ip2LocationScheduler 消费异常", e);
            }
        }
        log.info("Ip2LocationScheduler 消费线程已退出");
    }

    /**
     * TD-016: 应用关闭时优雅终止消费线程
     */
    @PreDestroy
    public void shutdown() {
        log.info("Ip2LocationScheduler 接收关闭信号，开始优雅关闭");
        running.set(false);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Ip2LocationScheduler 线程未在 5 秒内退出，执行强制关闭");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
        log.info("Ip2LocationScheduler 已关闭");
    }




}
