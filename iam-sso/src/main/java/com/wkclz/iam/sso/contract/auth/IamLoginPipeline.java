package com.wkclz.iam.sso.contract.auth;

import com.wkclz.auth.bean.*;
import com.wkclz.auth.contract.auth.*;
import com.wkclz.auth.enums.AuthErrorType;
import com.wkclz.auth.exception.AuthException;
import com.wkclz.iam.common.entity.IamLoginLog;
import com.wkclz.iam.sso.mapper.SsoLoginLogMapper;
import com.wkclz.web.helper.IpHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * IAM 登录管道 — 继承 sh-auth {@link LoginService}，实现 IAM 专属的抽象方法。
 * <p>
 * 负责：限流校验（1h 失败→需验证码，SSO 侧处理）、验证码校验、MFA、登录日志。
 * 标准认证流程（凭证校验 → 账号状态 → 会话创建）由父类 {@link LoginService#login} 模板方法驱动。
 * </p>
 *
 * @author shrimp
 */
@Slf4j
@Component
public class IamLoginPipeline extends LoginService {

    private final SsoLoginLogMapper ssoLoginLogMapper;

    @Autowired
    public IamLoginPipeline(AuthenticationProvider authenticationProvider,
                            TokenService tokenService,
                            SessionStore sessionStore,
                            CaptchaService captchaService,
                            RateLimitChecker rateLimitChecker,
                            MfaService mfaService,
                            ConcurrentSessionControl concurrentSessionControl,
                            StandardLoginPipeline pipeline,
                            SsoLoginLogMapper ssoLoginLogMapper) {
        this.authenticationProvider = authenticationProvider;
        this.tokenService = tokenService;
        this.sessionStore = sessionStore;
        this.captchaService = captchaService;
        this.rateLimitChecker = rateLimitChecker;
        // IAM 中账号状态已在 IamPasswordAuthenticationProvider.authenticate() 中校验，无需重复查询
        this.accountStatusChecker = subjectId -> {
        };
        this.mfaService = mfaService;
        this.concurrentSessionControl = concurrentSessionControl;
        this.pipeline = pipeline;
        this.ssoLoginLogMapper = ssoLoginLogMapper;
    }

    /**
     * 限流校验 — IAM 不使用独立限流，验证码机制（1h 失败次数）在 SSO 侧 IamLoginService 中提前判断
     */
    @Override
    protected void checkRateLimit(AuthRequest request, HttpServletRequest httpRequest) {
        // no-op: IAM 用验证码机制代替独立限流
    }

    /**
     * 验证码校验 — 若请求携带 captchaId，则从 Redis 校验
     */
    @Override
    protected void checkCaptcha(AuthRequest request) {
        Credential credential = request.getCredential();
        if (credential == null) return;
        String captchaId = credential.getCaptchaId();
        String captchaCode = credential.getCaptchaCode();
        if (StringUtils.isNotBlank(captchaId)) {
            if (!captchaService.verify(captchaId, captchaCode)) {
                log.warn("验证码错误, captchaId={}", captchaId);
                throw new AuthException(AuthErrorType.CAPTCHA_ERROR, "验证码错误");
            }
            log.debug("验证码校验通过, captchaId={}", captchaId);
        }
    }

    /**
     * MFA 检查 — 暂不支持
     */
    @Override
    protected MfaChallenge checkMfa(Principal principal) {
        return null;
    }

    /**
     * 登录日志记录 — 持久化到 iam_login_log 表
     */
    @Override
    protected void recordLoginLog(AuthResult result, AuthRequest request, HttpServletRequest httpRequest) {
        IamLoginLog loginLog = new IamLoginLog();
        loginLog.setAuthType("PASSWORD");

        // 从 AuthRequest.extra 获取用户名（用于失败场景 principal 为 null 时）
        String username = null;
        if (request != null && request.getExtra() != null) {
            username = (String) request.getExtra().get("username");
        }

        Principal principal = result.getPrincipal();
        if (principal != null) {
            loginLog.setUserCode(principal.getUserCode());
            loginLog.setUsername(principal.getUsername());
            loginLog.setAuthIdentifier(principal.getAuthIdentifier());
        } else if (username != null) {
            loginLog.setAuthIdentifier(username);
        }

        if (result.isSuccess()) {
            loginLog.setLoginStatus(0);
            loginLog.setMessage("登录成功");
        } else {
            loginLog.setLoginStatus(result.getErrorType() != null ? result.getErrorType().getCode() : 50);
            loginLog.setMessage(result.getErrorMessage() != null ? result.getErrorMessage() : "登录失败");
        }

        loginLog.setCreateBy(principal != null ? principal.getUsername() : username);
        loginLog.setUpdateBy(principal != null ? principal.getUsername() : username);

        // 从请求上下文获取 IP/UA
        if (httpRequest != null) {
            loginLog.setIpAddress(IpHelper.getOriginIp(httpRequest));
            loginLog.setUserAgent(httpRequest.getHeader("User-Agent"));
        }
        ssoLoginLogMapper.insertLoginLog(loginLog);
        log.debug("登录日志已记录, authIdentifier={}, status={}", loginLog.getAuthIdentifier(), loginLog.getLoginStatus());
    }
}
