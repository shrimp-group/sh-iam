package com.wkclz.iam.sso.service;

import com.wkclz.iam.session.enums.DestroyReason;
import com.wkclz.iam.session.service.SessionManager;
import com.wkclz.tool.tools.Md5Tool;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IamSessionService {

    private static final Logger log = LoggerFactory.getLogger(IamSessionService.class);

    @Autowired
    private SessionManager sessionManager;

    /**
     * 批量失效用户所有会话（旧版兼容方法）。
     * <p>
     * 已由 {@link SessionManager#destroyAllSessions(String, DestroyReason)} 替代。
     * 当前实现委托给 SessionManager，subjectId 暂用 username 传入（旧版调用方未传 userCode）。
     * </p>
     *
     * @deprecated 请改用 SessionManager.destroyAllSessions(subjectId, reason)
     */
    @Deprecated
    public void invalidateAllSessions(String username) {
        int count = sessionManager.destroyAllSessions(username, DestroyReason.PASSWORD_CHANGED);
        log.info("用户 {} 的所有会话已失效，共清理 {} 个会话", username, count);
    }

    /**
     * 本地登出：根据 token 销毁对应的 Redis 会话。
     *
     * @param token JWT token
     */
    public void logout(String token) {
        if (StringUtils.isBlank(token)) {
            log.warn("logout 传入 token 为空，跳过登出处理");
            return;
        }
        String sessionId = Md5Tool.md5(token);
        boolean destroyed = sessionManager.destroySession(sessionId, DestroyReason.LOGOUT);
        if (destroyed) {
            log.info("本地登出成功, sessionId={}", sessionId);
        } else {
            log.warn("本地登出: 会话不存在或已过期, sessionId={}", sessionId);
        }
    }

}
