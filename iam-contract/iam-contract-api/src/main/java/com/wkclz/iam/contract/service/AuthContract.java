package com.wkclz.iam.contract.service;

import com.wkclz.iam.contract.bean.AuthResult;
import com.wkclz.iam.contract.bean.Session;
import com.wkclz.iam.contract.enums.AuthErrorType;
import com.wkclz.iam.contract.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * 认证契约
 * 实现方负责从 HTTP 请求或 token 中认证用户，返回 Principal + Session
 *
 * @author shrimp
 */
public interface AuthContract {

    /**
     * 从 HTTP 请求中认证用户（过滤器主入口）
     * <p>
     * 实现职责：
     * 1. 从请求头提取 token（Authorization / token，去 Bearer 前缀）
     * 2. 校验 JWT 签名与有效期
     * 3. 校验 Session 存在性（如 Redis）
     * 4. 返回 Principal + Session
     *
     * @param request HTTP 请求
     * @return 认证结果；token 不存在时返回 null（由过滤器处理 public 路径放行）
     * @throws AuthException token 无效、签名错误、会话过期等
     */
    AuthResult authenticate(HttpServletRequest request);

    /**
     * 从 token 中认证用户（核心认证逻辑）
     * <p>
     * 由实现方提供 JWT 解析 + Session 获取的实际逻辑，
     * 供 {@link #checkToken(String, String)} 模板方法调用。
     * 与 {@link #authenticate(HttpServletRequest)} 的认证逻辑应保持一致，
     * 区别在于 token 已由调用方传入，无需从请求头提取。
     *
     * @param token JWT token
     * @return 认证结果
     * @throws AuthException token 无效、签名错误、会话过期等
     */
    AuthResult doAuthenticate(String token);

    /**
     * 校验 token（非 HTTP 请求场景：WebSocket、定时任务等）
     * <p>
     * 模板方法，内置以下通用校验：
     * <ol>
     *   <li>token 空值 → {@link AuthErrorType#TOKEN_MISSING}</li>
     *   <li>调用 {@link #doAuthenticate(String)} 获取认证结果</li>
     *   <li>Session 为空 → {@link AuthErrorType#SESSION_EXPIRED}</li>
     *   <li>authIdentifier 不一致 → {@link AuthErrorType#TOKEN_INVALID}</li>
     * </ol>
     *
     * @param token          JWT token
     * @param authIdentifier 认证标识符（用户名 / 三方平台标识），为空则跳过一致性校验
     * @return 会话信息
     * @throws AuthException 校验失败
     */
    default Session checkToken(String token, String authIdentifier) {
        // 1. token 空值校验
        if (!StringUtils.hasText(token)) {
            throw new AuthException(AuthErrorType.TOKEN_MISSING, "token 不能为空");
        }

        // 2. 核心：JWT 解析 + Session 获取（实现方提供）
        AuthResult authResult = doAuthenticate(token);

        // 3. Session 存在性校验
        if (authResult == null || authResult.getSession() == null) {
            throw new AuthException(AuthErrorType.SESSION_EXPIRED, "会话已过期");
        }

        // 4. authIdentifier 一致性校验
        if (StringUtils.hasText(authIdentifier)) {
            boolean passwordMatch = authIdentifier.equals(authResult.getPrincipal() != null
                ? authResult.getPrincipal().getUsername() : null);
            boolean thirdPartyMatch = authIdentifier.equals(authResult.getSession().getAuthIdentifier());
            if (!passwordMatch && !thirdPartyMatch) {
                throw new AuthException(AuthErrorType.TOKEN_INVALID, "认证标识不匹配");
            }
        }

        return authResult.getSession();
    }
}
