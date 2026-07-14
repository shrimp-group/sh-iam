package com.wkclz.auth.contract.auth;

import com.wkclz.auth.bean.*;
import com.wkclz.auth.enums.AuthErrorType;
import com.wkclz.auth.enums.AuthStatus;
import com.wkclz.auth.exception.AccountStatusException;
import com.wkclz.auth.exception.AuthenticationException;
import com.wkclz.auth.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
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

    public AuthResult login(AuthRequest request, HttpServletRequest httpRequest) {
        String identifier = resolveIdentifier(request, httpRequest);
        try {
            checkRateLimit(request, httpRequest);
            checkCaptcha(request);
            AuthResult result = authenticationProvider.authenticate(request, httpRequest);
            if (!result.isSuccess()) {
                rateLimitChecker.recordAttempt(identifier, false);
                recordLoginLog(result, httpRequest);
                return result;
            }
            accountStatusChecker.checkStatus(result.getPrincipal().getUserCode());
            MfaChallenge challenge = checkMfa(result.getPrincipal());
            if (challenge != null) {
                result.setStatus(AuthStatus.MFA_REQUIRED);
                result.setMfaChallenge(challenge);
                return result;
            }
            AuthToken token = tokenService.generateToken(result.getPrincipal());
            result.setToken(token);
            result.setStatus(AuthStatus.SUCCESS);
            Session session = createSession(result.getPrincipal(), token, httpRequest);
            concurrentSessionControl.enforce(session.getSubjectId());
            sessionStore.save(session);
            rateLimitChecker.recordAttempt(identifier, true);
            recordLoginLog(result, httpRequest);
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
        } catch (Exception e) {
            log.error("登录异常", e);
            return AuthResult.fail(AuthErrorType.INTERNAL_ERROR, "系统繁忙，请稍后重试");
        }
    }

    protected String resolveIdentifier(AuthRequest request, HttpServletRequest httpRequest) {
        return httpRequest.getRemoteAddr();
    }

    protected abstract void checkRateLimit(AuthRequest request, HttpServletRequest httpRequest);
    protected abstract void checkCaptcha(AuthRequest request);
    protected abstract MfaChallenge checkMfa(Principal principal);
    protected abstract Session createSession(Principal principal, AuthToken token, HttpServletRequest httpRequest);
    protected abstract void recordLoginLog(AuthResult result, HttpServletRequest httpRequest);
}
