package com.wkclz.auth.contract;

import com.wkclz.auth.bean.*;
import com.wkclz.auth.contract.auth.*;
import com.wkclz.auth.enums.AuthStatus;
import com.wkclz.auth.exception.AccountStatusException;
import com.wkclz.auth.exception.AuthenticationException;
import com.wkclz.auth.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    private AuthenticationProvider authProvider;
    @Mock
    private TokenService tokenService;
    @Mock
    private SessionStore sessionStore;
    @Mock
    private RateLimitChecker rateLimitChecker;
    @Mock
    private CaptchaService captchaService;
    @Mock
    private AccountStatusChecker accountStatusChecker;
    @Mock
    private MfaService mfaService;
    @Mock
    private ConcurrentSessionControl concurrentSessionControl;

    private LoginService loginService;
    private Principal principal;
    private AuthToken authToken;

    @BeforeEach
    void setUp() {
        // 创建 StandardLoginPipeline mock，返回预置结果
        StandardLoginPipeline pipeline = new StandardLoginPipeline(
            tokenService, sessionStore, concurrentSessionControl);

        loginService = new TestLoginService(
                authProvider, tokenService, sessionStore, rateLimitChecker,
            captchaService, accountStatusChecker, mfaService, concurrentSessionControl,
            pipeline);

        principal = new Principal();
        principal.setUserCode("user_001");
        principal.setUsername("admin");

        authToken = new AuthToken();
        authToken.setTokenValue("test-jwt-token");
    }

    @Test
    void testLoginSuccess() {
        AuthResult authResult = new AuthResult();
        authResult.setStatus(AuthStatus.SUCCESS);
        authResult.setPrincipal(principal);

        when(authProvider.authenticate(any(), any())).thenReturn(authResult);
        when(mfaService.isMfaRequired(anyString())).thenReturn(false);
        when(tokenService.generateToken(principal)).thenReturn(authToken);

        AuthResult result = loginService.login(new AuthRequest(), mock(HttpServletRequest.class));

        assertTrue(result.isSuccess());
        assertEquals(authToken, result.getToken());
        verify(rateLimitChecker).recordAttempt(anyString(), eq(true));
        verify(sessionStore).save(any());
    }

    @Test
    void testLoginBadCredentials() {
        when(authProvider.authenticate(any(), any()))
                .thenThrow(new AuthenticationException(null, "密码错误"));

        AuthResult result = loginService.login(new AuthRequest(), mock(HttpServletRequest.class));

        assertFalse(result.isSuccess());
        verify(rateLimitChecker).recordAttempt(anyString(), eq(false));
    }

    @Test
    void testLoginRateLimited() {
        doThrow(new RateLimitException("频率超限")).when(rateLimitChecker).check(anyString());

        AuthResult result = loginService.login(new AuthRequest(), mock(HttpServletRequest.class));

        assertFalse(result.isSuccess());
        verify(authProvider, never()).authenticate(any(), any());
    }

    @Test
    void testLoginAccountDisabled() {
        AuthResult authResult = new AuthResult();
        authResult.setStatus(AuthStatus.SUCCESS);
        authResult.setPrincipal(principal);

        when(authProvider.authenticate(any(), any())).thenReturn(authResult);
        doThrow(new AccountStatusException(null, "账号已禁用"))
                .when(accountStatusChecker).checkStatus("user_001");

        AuthResult result = loginService.login(new AuthRequest(), mock(HttpServletRequest.class));

        assertFalse(result.isSuccess());
    }

    @Test
    void testLoginMfaRequired() {
        AuthResult authResult = new AuthResult();
        authResult.setStatus(AuthStatus.SUCCESS);
        authResult.setPrincipal(principal);

        MfaChallenge challenge = new MfaChallenge();
        challenge.setChallengeId("challenge-001");

        when(authProvider.authenticate(any(), any())).thenReturn(authResult);
        when(mfaService.isMfaRequired(anyString())).thenReturn(true);
        when(mfaService.sendChallenge(anyString(), any())).thenReturn(challenge);

        AuthResult result = loginService.login(new AuthRequest(), mock(HttpServletRequest.class));

        assertEquals(AuthStatus.MFA_REQUIRED, result.getStatus());
        assertNotNull(result.getMfaChallenge());
    }

    // Test 子类实现 — 通过 StandardLoginPipeline 间接使用 tokenService/sessionStore/concurrentSessionControl
    static class TestLoginService extends LoginService {
        TestLoginService(AuthenticationProvider ap, TokenService ts, SessionStore ss,
                         RateLimitChecker rlc, CaptchaService cs, AccountStatusChecker asc,
                         MfaService ms, ConcurrentSessionControl csc,
                         StandardLoginPipeline pipeline) {
            this.authProvider = ap;
            this.tokenService = ts;
            this.sessionStore = ss;
            this.rateLimitChecker = rlc;
            this.captchaService = cs;
            this.accountStatusChecker = asc;
            this.mfaService = ms;
            this.concurrentSessionControl = csc;
            this.pipeline = pipeline;
        }

        @Override
        protected void checkRateLimit(AuthRequest request, HttpServletRequest httpRequest) {
            rateLimitChecker.check(httpRequest.getRemoteAddr());
        }

        @Override
        protected void checkCaptcha(AuthRequest request) {}

        @Override
        protected MfaChallenge checkMfa(Principal principal) {
            if (mfaService.isMfaRequired(principal.getUserCode())) {
                return mfaService.sendChallenge(principal.getUserCode(), null);
            }
            return null;
        }

        @Override
        protected void recordLoginLog(AuthResult result, AuthRequest request, HttpServletRequest httpRequest) {
        }
    }
}
