package com.wkclz.iam.sso.schedule;


import com.wkclz.iam.common.entity.IamRequestLog;
import com.wkclz.iam.common.helper.IpLocalCacheHelper;
import com.wkclz.iam.sso.mapper.SsoRequestLogMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author shrimp
 */
@Slf4j
@Component
public class Ip2LocationScheduler implements ApplicationRunner {

    private volatile boolean running = true;

    @Resource
    private SsoRequestLogMapper ssoRequestLogMapper;

    // 消化队列
    @Override
    public void run(ApplicationArguments args) {
        new Thread(() -> {
            while (running) {
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
                    break;
                }
            }
        }, "ip2addr").start();
    }




}