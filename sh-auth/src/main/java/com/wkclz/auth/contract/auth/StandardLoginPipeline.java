package com.wkclz.auth.contract.auth;

import com.wkclz.auth.bean.AuthToken;
import com.wkclz.auth.bean.Principal;
import com.wkclz.auth.bean.Session;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 标准登录管道 — 通用的"认证成功后"会话创建流程。
 * <p>
 * 职责：Token 生成 → Session 构建 → 持久化存储 → 并发会话控制。
 * 不包含凭证校验、验证码、限流、MFA 等认证前逻辑，仅处理认证通过后的标准流程。
 * </p>
 * <p>
 * 使用场景：
 * <ul>
 *   <li>sh-auth LoginService 模板方法中的成功分支</li>
 *   <li>iam-sso LocalSsoFacadeContract 中的会话创建</li>
 *   <li>任何已完成凭证验证的场景</li>
 * </ul>
 * </p>
 *
 * @author shrimp
 */
@Slf4j
@Component
@AllArgsConstructor
public class StandardLoginPipeline {

    private final TokenService tokenService;
    private final SessionStore sessionStore;
    private final ConcurrentSessionControl concurrentSessionControl;


    /**
     * 执行标准登录管道：生成 Token → 创建 Session → 持久化 → 并发控制
     *
     * @param principal 已认证的用户主体
     * @param authType  认证类型（如 PASSWORD、LDAP）
     * @param clientIp  客户端 IP（可选）
     * @param userAgent 客户端 UserAgent（可选）
     * @return 包含 token 和 session 的结果
     */
    public PipelineResult execute(Principal principal, String authType, String clientIp, String userAgent) {
        log.info("标准登录管道执行, userCode={}, authType={}", principal.getUserCode(), authType);

        // 1. 生成 JWT Token
        AuthToken authToken = tokenService.generateToken(principal);
        String tokenValue = authToken.getTokenValue();
        log.debug("Token 已生成, tokenPreview={}...", tokenValue.substring(0, Math.min(8, tokenValue.length())));

        // 2. 构建 Session
        Session session = buildSession(principal, authType, tokenValue, clientIp, userAgent);

        // 3. 持久化 Session
        sessionStore.save(session);
        log.debug("Session 已持久化, subjectId={}", session.getSubjectId());

        // 4. 并发会话控制（超出上限踢出最早会话）
        concurrentSessionControl.enforce(session.getSubjectId());

        return new PipelineResult(authToken, session);
    }

    private Session buildSession(Principal principal, String authType,
                                 String tokenValue, String clientIp, String userAgent) {
        Session session = new Session();
        session.setSessionId(tokenValue);
        session.setSubjectId(principal.getUsername());
        session.setPrincipal(principal);
        session.setAuthType(authType);
        session.setAuthIdentifier(principal.getAuthIdentifier());
        session.setCreateTime(LocalDateTime.now());
        if (clientIp != null) {
            session.setClientIp(clientIp);
        }
        if (userAgent != null) {
            session.setUserAgent(userAgent);
        }
        return session;
    }

    /**
     * 管道执行结果
     */
    public static class PipelineResult {
        private final AuthToken authToken;
        private final Session session;

        public PipelineResult(AuthToken authToken, Session session) {
            this.authToken = authToken;
            this.session = session;
        }

        public AuthToken getAuthToken() {
            return authToken;
        }

        public Session getSession() {
            return session;
        }

        public String getTokenValue() {
            return authToken.getTokenValue();
        }
    }
}
