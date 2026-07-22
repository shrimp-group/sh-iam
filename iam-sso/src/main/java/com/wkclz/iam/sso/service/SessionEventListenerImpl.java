package com.wkclz.iam.sso.service;

import com.alibaba.fastjson2.JSON;
import com.wkclz.core.identity.UserIdentity;
import com.wkclz.iam.common.entity.IamLoginLog;
import com.wkclz.iam.session.bean.Session;
import com.wkclz.iam.session.enums.DestroyReason;
import com.wkclz.iam.session.enums.LoginStatus;
import com.wkclz.iam.session.event.SessionEvent;
import com.wkclz.iam.session.event.SessionEventListener;
import com.wkclz.iam.sso.event.LoginEvent;
import com.wkclz.iam.sso.mapper.SsoLoginLogMapper;
import com.wkclz.web.helper.IpHelper;
import com.wkclz.web.helper.RequestHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 会话审计事件监听器实现 — 将会话生命周期事件和登录事件写入 iam_login_log 表。
 *
 * <p>实现 {@link SessionEventListener} 处理会话创建/销毁/过期，
 * 同时通过 {@code @EventListener} 监听 {@link LoginEvent} 处理登录失败。</p>
 * <p>登录成功由 {@link #onCreated(Session)} 处理，{@link LoginEvent.Type#SUCCESS} 不做重复记录。</p>
 */
@Component
public class SessionEventListenerImpl implements SessionEventListener {

    private static final Logger log = LoggerFactory.getLogger(SessionEventListenerImpl.class);

    @Autowired
    private SsoLoginLogMapper ssoLoginLogMapper;

    // ========== SessionEvent 桥接（Spring Event → 接口方法） ==========

    /**
     * 监听 {@link SessionEvent}，按类型分派到 {@link SessionEventListener} 接口方法。
     */
    @EventListener
    public void onSessionEvent(SessionEvent event) {
        switch (event.getType()) {
            case CREATED -> {
                if (event.getSession() != null) {
                    onCreated(event.getSession());
                }
            }
            case DESTROYED -> onDestroyed(event.getSessionId(), event.getSubjectId(), event.getReason());
            case EXPIRED -> onExpired(event.getSessionId(), event.getSubjectId());
        }
    }

    // ========== SessionEventListener 接口实现 ==========

    @Override
    public void onCreated(Session session) {
        try {
            IamLoginLog loginLog = new IamLoginLog();
            loginLog.setUserCode(session.getSubjectId());
            loginLog.setAuthType(session.getAuthType() != null ? session.getAuthType().name() : null);
            loginLog.setLoginStatus(LoginStatus.SUCCESS.getCode());
            loginLog.setMessage(LoginStatus.SUCCESS.getMessage());

            // 从 UserIdentity JSON 中获取 username
            parseUserIdentity(session.getUserIdentity(), loginLog);

            // 从当前请求获取客户端信息
            HttpServletRequest request = RequestHelper.getRequest();
            if (request != null) {
                loginLog.setIpAddress(IpHelper.getOriginIp(request));
                loginLog.setUserAgent(request.getHeader("User-Agent"));
            }

            loginLog.setCreateBy(loginLog.getUsername() != null ? loginLog.getUsername() : session.getSubjectId());
            loginLog.setUpdateBy(loginLog.getUsername() != null ? loginLog.getUsername() : session.getSubjectId());

            ssoLoginLogMapper.insertLoginLog(loginLog);
            log.info("登录日志记录成功: username={}, ip={}", loginLog.getUsername(), loginLog.getIpAddress());
        } catch (Exception e) {
            log.error("onCreated 审计日志写入失败: sessionId={}, error={}", session.getSessionId(), e.getMessage(), e);
        }
    }

    @Override
    public void onDestroyed(String sessionId, String subjectId, DestroyReason reason) {
        try {
            IamLoginLog loginLog = new IamLoginLog();
            loginLog.setAuthIdentifier(subjectId);
            loginLog.setUserCode(subjectId);
            loginLog.setMessage(reason.name());

            // 根据销毁原因设置状态
            String statusMessage = switch (reason) {
                case LOGOUT -> "用户主动登出";
                case PASSWORD_CHANGED -> "密码修改导致会话失效";
                case PASSWORD_RESET_BY_ADMIN -> "管理员重置密码导致会话失效";
                case USER_DISABLED -> "用户被禁用导致会话失效";
                case CONCURRENT_KICK -> "并发会话踢出";
                case SESSION_EXPIRED -> "会话自然过期";
            };
            loginLog.setMessage(statusMessage);
            loginLog.setCreateBy(subjectId);
            loginLog.setUpdateBy(subjectId);

            ssoLoginLogMapper.insertLoginLog(loginLog);
            log.info("会话销毁日志记录: subjectId={}, reason={}", subjectId, reason);
        } catch (Exception e) {
            log.error("onDestroyed 审计日志写入失败: sessionId={}, subjectId={}, reason={}, error={}",
                sessionId, subjectId, reason, e.getMessage(), e);
        }
    }

    @Override
    public void onExpired(String sessionId, String subjectId) {
        try {
            IamLoginLog loginLog = new IamLoginLog();
            loginLog.setAuthIdentifier(subjectId);
            loginLog.setUserCode(subjectId);
            loginLog.setMessage("会话自然过期");
            loginLog.setCreateBy(subjectId);
            loginLog.setUpdateBy(subjectId);

            ssoLoginLogMapper.insertLoginLog(loginLog);
            log.info("会话过期日志记录: subjectId={}, sessionId={}", subjectId, masked(sessionId));
        } catch (Exception e) {
            log.error("onExpired 审计日志写入失败: sessionId={}, subjectId={}, error={}",
                sessionId, subjectId, e.getMessage(), e);
        }
    }

    // ========== LoginEvent 监听 ==========

    /**
     * 监听登录失败事件，写入审计日志。
     * 登录成功由 {@link #onCreated(Session)} 处理，此处只处理失败。
     */
    @EventListener
    public void onLoginEvent(LoginEvent event) {
        if (event.getType() != LoginEvent.Type.FAILED) {
            return;
        }
        try {
            IamLoginLog loginLog = new IamLoginLog();
            loginLog.setAuthIdentifier(event.getUsername());
            loginLog.setUsername(event.getUsername());
            loginLog.setLoginStatus(event.getLoginStatus().getCode());
            loginLog.setMessage(event.getLoginStatus().getMessage());
            loginLog.setIpAddress(event.getIp());
            loginLog.setUserAgent(event.getUserAgent());
            loginLog.setCreateBy(event.getUsername());
            loginLog.setUpdateBy(event.getUsername());

            ssoLoginLogMapper.insertLoginLog(loginLog);
            log.info("登录失败日志记录: username={}, loginStatus={}, ip={}",
                event.getUsername(), event.getLoginStatus(), event.getIp());
        } catch (Exception e) {
            log.error("登录失败审计日志写入失败: username={}, loginStatus={}, error={}",
                event.getUsername(), event.getLoginStatus(), e.getMessage(), e);
        }
    }

    // ========== 内部方法 ==========

    /**
     * 从 UserIdentity JSON 中提取 username/nickname 等字段回填到 IamLoginLog。
     */
    private static void parseUserIdentity(String userIdentityJson, IamLoginLog loginLog) {
        if (userIdentityJson == null || userIdentityJson.isEmpty()) {
            return;
        }
        try {
            UserIdentity identity = JSON.parseObject(userIdentityJson, UserIdentity.class);
            if (identity != null) {
                loginLog.setUsername(identity.getUsername());
                loginLog.setAuthIdentifier(identity.getUsername());
            }
        } catch (Exception e) {
            log.debug("UserIdentity JSON 解析失败: {}", e.getMessage());
        }
    }

    /**
     * sessionId 脱敏 — 仅保留前 8 位。
     */
    private static String masked(String sessionId) {
        if (sessionId == null || sessionId.length() <= 8) {
            return sessionId == null ? "" : sessionId;
        }
        return sessionId.substring(0, 8) + "****";
    }
}
