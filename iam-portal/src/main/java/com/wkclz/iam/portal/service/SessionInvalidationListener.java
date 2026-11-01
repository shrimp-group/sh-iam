package com.wkclz.iam.portal.service;

import com.wkclz.iam.common.event.AdminSecurityEvent;
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
 * <p>监听 {@link AdminSecurityEvent}，按类型分发处理：
 * <ul>
 *   <li>PASSWORD_RESET → destroyAllSessions(PASSWORD_RESET_BY_ADMIN)</li>
 *   <li>STATUS_CHANGED → destroyAllSessions(USER_DISABLED)</li>
 * </ul>
 * 所有方法 try-catch 包裹，异常不影响 iam-admin 主流程。
 * 用户角色变更不在此处理——由 PermissionChangeEvent 经权限缓存清理链路接管。</p>
 */
@Component
public class SessionInvalidationListener {

    private static final Logger log = LoggerFactory.getLogger(SessionInvalidationListener.class);

    @Autowired
    private SessionManager sessionManager;

    /**
     * 监听管理员安全操作事件，按类型分发处理。
     */
    @EventListener
    public void onAdminSecurityEvent(AdminSecurityEvent event) {
        switch (event.getType()) {
            case PASSWORD_RESET -> handlePasswordReset(event);
            case STATUS_CHANGED -> handleStatusChanged(event);
        }
    }

    private void handlePasswordReset(AdminSecurityEvent event) {
        try {
            log.info("收到管理员重置密码事件, userCode={}", event.getUserCode());
            sessionManager.destroyAllSessions(event.getUserCode(), DestroyReason.PASSWORD_RESET_BY_ADMIN);
            log.info("管理员重置密码 — 会话已全部销毁: userCode={}", event.getUserCode());
        } catch (Exception e) {
            log.error("管理员重置密码会话销毁失败: userCode={}, error={}", event.getUserCode(), e.getMessage(), e);
        }
    }

    private void handleStatusChanged(AdminSecurityEvent event) {
        try {
            log.info("收到用户状态变更事件, userCode={}, newStatus={}", event.getUserCode(), event.getNewStatus());
            sessionManager.destroyAllSessions(event.getUserCode(), DestroyReason.USER_DISABLED);
            log.info("用户状态变更 — 会话已全部销毁: userCode={}", event.getUserCode());
        } catch (Exception e) {
            log.error("用户状态变更会话销毁失败: userCode={}, newStatus={}, error={}",
                event.getUserCode(), event.getNewStatus(), e.getMessage(), e);
        }
    }
}
