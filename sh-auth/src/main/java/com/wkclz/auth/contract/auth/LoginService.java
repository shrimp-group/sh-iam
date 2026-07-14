package com.wkclz.auth.contract.auth;

import com.wkclz.auth.bean.*;
import com.wkclz.auth.enums.AuthErrorType;
import com.wkclz.auth.enums.AuthStatus;
import com.wkclz.auth.exception.AccountStatusException;
import com.wkclz.auth.exception.AuthException;
import com.wkclz.auth.exception.AuthenticationException;
import com.wkclz.auth.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 登录服务模板方法 — 定义标准登录流程，与 SSO 无关。
 * <p>
 * 流程编排：
 * <ol>
 *   <li>限流校验 → {@link #checkRateLimit}</li>
 *   <li>验证码校验 → {@link #checkCaptcha}</li>
 *   <li>凭证认证 → {@link AuthenticationProvider#authenticate}</li>
 *   <li>账号状态检查 → {@link AccountStatusChecker#checkStatus}</li>
 *   <li>MFA 检查 → {@link #checkMfa}</li>
 *   <li>会话创建 → {@link StandardLoginPipeline#execute}</li>
 *   <li>登录日志 → {@link #recordLoginLog}</li>
 * </ol>
 * </p>
 * <p>
 * 子类只需实现 4 个抽象方法（限流、验证码、MFA、日志），
 * 核心认证编排逻辑由本类统一管理，不包含 SSO 特定逻辑。
 * </p>
 *
 * @author shrimp
 */
@Slf4j
public abstract class LoginService {

    protected MfaService mfaService;
    protected TokenService tokenService;
    protected SessionStore sessionStore;
    protected CaptchaService captchaService;
    protected RateLimitChecker rateLimitChecker;
    protected AccountStatusChecker accountStatusChecker;
    protected AuthenticationProvider authenticationProvider;
    protected ConcurrentSessionControl concurrentSessionControl;
    /**
     * 标准登录管道 — 处理认证成功后的 Token 生成、Session 创建、并发控制
     */
    protected StandardLoginPipeline pipeline;

    /**
     * 标准登录流程入口
     *
     * @param request     认证请求
     * @param httpRequest HTTP 请求
     * @return 认证结果
     */
    public AuthResult login(AuthRequest request, HttpServletRequest httpRequest) {
        String identifier = httpRequest.getRemoteAddr();
        try {
            // 限流校验
            checkRateLimit(request, httpRequest);
            // 验证码校验
            checkCaptcha(request);
            // 校验用户信息
            AuthResult result = authenticationProvider.authenticate(request, httpRequest);
            if (!result.isSuccess()) {
                rateLimitChecker.recordAttempt(identifier, false);
                recordLoginLog(result, request, httpRequest);
                return result;
            }
            accountStatusChecker.checkStatus(result.getPrincipal().getUserCode());
            MfaChallenge challenge = checkMfa(result.getPrincipal());
            if (challenge != null) {
                result.setStatus(AuthStatus.MFA_REQUIRED);
                result.setMfaChallenge(challenge);
                return result;
            }
            // 使用标准登录管道创建会话（Token → Session → 持久化 → 并发控制）
            StandardLoginPipeline.PipelineResult pipelineResult = pipeline.execute(
                result.getPrincipal(),
                request.getAuthType(),
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
            );
            result.setToken(pipelineResult.getAuthToken());
            result.setSession(pipelineResult.getSession());
            result.setStatus(AuthStatus.SUCCESS);
            rateLimitChecker.recordAttempt(identifier, true);
            recordLoginLog(result, request, httpRequest);
            return result;
        } catch (AuthenticationException e) {
            log.warn("认证失败: {}", e.getMessage());
            rateLimitChecker.recordAttempt(identifier, false);
            return AuthResult.fail(AuthErrorType.BAD_CREDENTIALS, e.getMessage());
        } catch (RateLimitException e) {
            log.warn("登录频率超限: {}", e.getMessage());
            return AuthResult.fail(AuthErrorType.RATE_LIMITED, e.getMessage());
        } catch (AccountStatusException e) {
            log.warn("账号状态异常: {}", e.getMessage());
            return AuthResult.fail(AuthErrorType.ACCOUNT_DISABLED, e.getMessage());
        } catch (AuthException e) {
            log.warn("认证流程异常: type={}, msg={}", e.getErrorType(), e.getMessage());
            rateLimitChecker.recordAttempt(identifier, false);
            recordLoginLog(AuthResult.fail(e.getErrorType(), e.getMessage()), request, httpRequest);
            return AuthResult.fail(e.getErrorType(), e.getMessage());
        } catch (Exception e) {
            log.error("登录异常", e);
            return AuthResult.fail(AuthErrorType.INTERNAL_ERROR, "系统繁忙，请稍后重试");
        }
    }


    /**
     * 限流校验（子类实现具体策略）
     */
    protected abstract void checkRateLimit(AuthRequest request, HttpServletRequest httpRequest);

    /**
     * 验证码校验（子类实现具体策略）
     */
    protected abstract void checkCaptcha(AuthRequest request);

    /**
     * MFA 多因素认证检查（子类实现具体策略）
     */
    protected abstract MfaChallenge checkMfa(Principal principal);

    /**
     * 登录日志记录（子类实现持久化策略）
     */
    protected abstract void recordLoginLog(AuthResult result, AuthRequest request, HttpServletRequest httpRequest);
}
