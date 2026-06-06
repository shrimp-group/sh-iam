package com.wkclz.iam.admin.job;

import com.wkclz.iam.admin.service.IamUserRoleService;
import com.wkclz.redis.helper.RedisLock;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 用户角色有效期定时任务
 * 双触发入口：XXL-Job 和 Spring @Scheduled
 * 实际使用时可自行切换触发入口
 */
@Component
public class UserRoleExpireJobHandler {

    private static final Logger log = LoggerFactory.getLogger(UserRoleExpireJobHandler.class);

    private static final String LOCK_KEY = "iam:job:user-role-expire";
    private static final long LOCK_EXPIRE = 300;

    @Autowired
    private IamUserRoleService iamUserRoleService;

    @Autowired
    private RedisLock redisLock;

    @Value("${iam.job.user-role-expire.enabled:false}")
    private boolean scheduleEnabled;

    /**
     * XXL-Job 触发入口
     */
    @XxlJob("userRoleExpireJob")
    public void userRoleExpireJob() {
        XxlJobHelper.log("开始执行用户角色有效期刷新任务...");
        log.info("XXL-Job 触发: 用户角色有效期刷新任务开始");
        iamUserRoleService.refreshEnableStatus();
        log.info("XXL-Job 触发: 用户角色有效期刷新任务完成");
        XxlJobHelper.log("用户角色有效期刷新任务执行完成");
    }

    /**
     * Spring @Scheduled 触发入口
     * 受 iam.job.user-role-expire.enabled 开关控制，默认关闭
     * 使用 RedisLock 分布式锁保证集群中只有一个实例执行
     */
    @Scheduled(cron = "${iam.job.user-role-expire.cron:0 */5 * * * ?}")
    public void scheduledRefresh() {
        if (!scheduleEnabled) {
            return;
        }
        String lockId = redisLock.tryLock(LOCK_KEY, LOCK_EXPIRE, TimeUnit.SECONDS);
        if (lockId == null) {
            log.debug("Spring @Scheduled 触发: 未获取到分布式锁，跳过执行");
            return;
        }
        try {
            log.info("Spring @Scheduled 触发: 用户角色有效期刷新任务开始");
            iamUserRoleService.refreshEnableStatus();
            log.info("Spring @Scheduled 触发: 用户角色有效期刷新任务完成");
        } finally {
            redisLock.releaseLock(LOCK_KEY, lockId);
        }
    }

}
