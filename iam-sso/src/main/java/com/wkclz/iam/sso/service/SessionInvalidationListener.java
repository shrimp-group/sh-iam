package com.wkclz.iam.sso.service;

import com.wkclz.iam.common.event.PasswordResetByAdminEvent;
import com.wkclz.iam.common.event.UserRoleChangedEvent;
import com.wkclz.iam.common.event.UserStatusChangedEvent;
import com.wkclz.iam.session.enums.DestroyReason;
import com.wkclz.iam.session.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 管理员安全操作驱动的会话失效监听器。
 *
 * <p>监听管理员操作事件（重置密码、状态变更），调用 SessionManager 批量销毁用户会话。
 * 所有方法 try-catch 包裹，异常不影响 iam-admin 主流程。</p>
 */
@Component
public class SessionInvalidationListener {

    private static final Logger log = LoggerFactory.getLogger(SessionInvalidationListener.class);

    @Autowired
    private SessionManager sessionManager;

    /**
     * 管理员重置密码 → 全会话失效。
     */
    @EventListener
    public void onPasswordResetByAdmin(PasswordResetByAdminEvent event) {
        try {
            log.info("收到管理员重置密码事件, userCode={}", event.getUserCode());
            sessionManager.destroyAllSessions(event.getUserCode(), DestroyReason.PASSWORD_RESET_BY_ADMIN);
            log.info("管理员重置密码 — 会话已全部销毁: userCode={}", event.getUserCode());
        } catch (Exception e) {
            log.error("管理员重置密码会话销毁失败: userCode={}, error={}", event.getUserCode(), e.getMessage(), e);
        }
    }

    /**
     * 用户状态变更（禁用/锁定） → 全会话失效。
     */
    @EventListener
    public void onUserStatusChanged(UserStatusChangedEvent event) {
        try {
            log.info("收到用户状态变更事件, userCode={}, newStatus={}", event.getUserCode(), event.getNewStatus());
            sessionManager.destroyAllSessions(event.getUserCode(), DestroyReason.USER_DISABLED);
            log.info("用户状态变更 — 会话已全部销毁: userCode={}", event.getUserCode());
        } catch (Exception e) {
            log.error("用户状态变更会话销毁失败: userCode={}, newStatus={}, error={}",
                event.getUserCode(), event.getNewStatus(), e.getMessage(), e);
        }
    }

    /**
     * 角色绑定变更 — 不销毁会话，仅刷新权限缓存。
     * 当前 cache 机制尚未实现，仅记录日志。
     */
    @EventListener
    public void onUserRoleChanged(UserRoleChangedEvent event) {
        try {
            log.info("收到用户角色变更事件, userCode={}, 不销毁会话，待权限缓存实现", event.getUserCode());
            // TODO: 权限缓存刷新 → 发布 AuthCacheRefreshEvent
        } catch (Exception e) {
            log.error("用户角色变更处理失败: userCode={}, error={}", event.getUserCode(), e.getMessage(), e);
        }
    }
}
