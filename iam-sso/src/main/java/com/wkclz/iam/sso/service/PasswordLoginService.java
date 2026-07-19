package com.wkclz.iam.sso.service;

import com.wkclz.core.exception.UserException;
import com.wkclz.core.identity.UserIdentity;
import com.wkclz.iam.common.entity.IamLoginLog;
import com.wkclz.iam.common.entity.IamUserAuth;
import com.wkclz.iam.common.entity.IamUserAuthPassword;
import com.wkclz.iam.common.entity.IamUserPasswordHis;
import com.wkclz.iam.sdk.bean.enums.LoginStatus;
import com.wkclz.iam.sdk.bean.req.ChangePasswordReq;
import com.wkclz.iam.sdk.facade.SsoFacade;
import com.wkclz.iam.sdk.helper.SessionHelper;
import com.wkclz.iam.session.bean.SessionCreateResult;
import com.wkclz.iam.session.enums.AuthType;
import com.wkclz.iam.session.service.SessionManager;
import com.wkclz.iam.sso.bean.req.LoginReq;
import com.wkclz.iam.sso.bean.resp.LoginResp;
import com.wkclz.iam.sso.config.IamSsoConfig;
import com.wkclz.iam.sso.event.LoginFailedEvent;
import com.wkclz.iam.sso.event.LoginSuccessEvent;
import com.wkclz.iam.sso.mapper.SsoLoginLogMapper;
import com.wkclz.iam.sso.mapper.SsoLoginMapper;
import com.wkclz.iam.sso.spi.CredentialChecker;
import com.wkclz.iam.sso.spi.PasswordEncoder;
import com.wkclz.tool.tools.RsaTool;
import com.wkclz.web.helper.IpHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

@Service
public class PasswordLoginService {

    private static final Logger log = LoggerFactory.getLogger(PasswordLoginService.class);

    @Autowired
    private SessionManager sessionManager;
    @Autowired
    private SsoFacade ssoFacade;
    @Autowired
    private IamSsoConfig iamSsoConfig;
    @Autowired
    private SsoLoginMapper ssoLoginMapper;
    @Autowired
    private SsoLoginLogMapper ssoLoginLogMapper;
    @Autowired
    private IamSessionService iamSessionService;
    @Autowired
    private CredentialChecker credentialChecker;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /**
     * 密码登录。
     *
     * <p>阶段：密码解密 → 验证码判断 → 凭证校验 → 会话创建 → 返回 LoginResp。</p>
     */
    public LoginResp login(HttpServletRequest request, LoginReq loginReq) {
        String username = loginReq.getUsername();
        String captchaCode = loginReq.getCaptchaCode();
        String captchaId = loginReq.getCaptchaId();
        String clientIp = IpHelper.getOriginIp(request);
        String userAgent = request.getHeader("User-Agent");

        // 阶段1：密码解密
        String password = loginReq.getPassword();
        String privateKey = iamSsoConfig.getPrivateKey();
        if (StringUtils.isNotBlank(privateKey) && password.length() > 32) {
            password = RsaTool.decryptByPrivateKey(password, privateKey);
            log.debug("RSA 私钥解密密码完成");
        }

        // 阶段2：验证码判断
        IamLoginLog param = new IamLoginLog();
        param.setAuthIdentifier(username);
        param.setAuthType(AuthType.PASSWORD.name());
        IamLoginLog lastLoginIn1Hour = ssoLoginLogMapper.getLastLoginIn1Hour(param);
        if (lastLoginIn1Hour != null && lastLoginIn1Hour.getLoginStatus() != 0
                && (StringUtils.isBlank(captchaCode) || StringUtils.isBlank(captchaId))) {
            log.info("用户 {} 1h 内有失败记录，需要验证码", username);
            return failResp(LoginStatus.NEED_CAPTCHA, username, clientIp, userAgent);
        }

        // 验证码校验
        if (StringUtils.isNotBlank(captchaId)) {
            if (!captchaService.verify(captchaId, captchaCode)) {
                log.info("Captcha verify failed for user: {}", username);
                return failResp(LoginStatus.INVALID_CAPTCHA, username, clientIp, userAgent);
            }
        }

        // 阶段3：凭证校验（委托 CredentialChecker：存在 → 禁用 → 锁定 → 认证禁用 → 密码匹配 → 密码过期）
        CredentialCheckResult result = credentialChecker.check(username, password);

        if (!result.isSuccess()) {
            LoginStatus status = switch (result.getFailReason()) {
                case USER_NOT_FOUND -> LoginStatus.USER_NOT_FOUND;
                case AUTH_DISABLED -> LoginStatus.EXPIRED_ACCOUNT;
                case LOCKED -> LoginStatus.ACCOUNT_LOCKED;
                case DISABLED -> LoginStatus.ACCOUNT_DISABLED;
                case PASSWORD_ERROR -> LoginStatus.INVALID_CREDENTIALS;
                case PASSWORD_EXPIRED -> LoginStatus.EXPIRED_PASSWORD;
            };
            return failResp(status, username, clientIp, userAgent);
        }

        // 阶段4：登录成功 — 委托 SessionManager 创建会话
        UserIdentity userIdentity = result.getUserIdentity();
        log.info("用户 {} 认证成功，调用 SessionManager 创建会话", username);

        SessionCreateResult sessionResult = sessionManager.createSession(userIdentity, AuthType.PASSWORD);

        // 更新最后登录 IP
        IamUserAuth userAuth = new IamUserAuth();
        userAuth.setUserCode(userIdentity.getUserCode());
        userAuth.setAuthIdentifier(username);
        userAuth.setLastLoginIp(clientIp);
        ssoLoginMapper.updateUserLoginInfo(userAuth);

        // 发布登录成功事件
        eventPublisher.publishEvent(new LoginSuccessEvent(username, userIdentity.getUserCode(), clientIp, userAgent, sessionResult.getToken()));

        // 构建成功响应
        LoginResp resp = new LoginResp();
        resp.setLoginStatus(LoginStatus.SUCCESS.getCode());
        resp.setLoginMessage(LoginStatus.SUCCESS.getMessage());
        resp.setToken(sessionResult.getToken());
        resp.setUserCode(userIdentity.getUserCode());
        resp.setUsername(userIdentity.getUsername());
        resp.setNickname(userIdentity.getNickname());
        resp.setAvatar(userIdentity.getAvatar());
        return resp;
    }

    public void logout(HttpServletRequest request) {
        String token = SessionHelper.getToken(request);
        if (StringUtils.isBlank(token)) {
            return;
        }
        // 委托 SsoFacade 执行登出（US-13 将改为直接调用 SessionManager）
        ssoFacade.logout(token);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordReq request) {
        Assert.notNull(request.getOldPassword(), "旧密码不能为空");
        Assert.notNull(request.getNewPassword(), "新密码不能为空");

        String userCode = SessionHelper.getUserCode();
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        String privateKey = iamSsoConfig.getPrivateKey();
        if (StringUtils.isNotBlank(privateKey)) {
            if (oldPassword.length() > 32) {
                oldPassword = RsaTool.decryptByPrivateKey(oldPassword, privateKey);
            }
            if (newPassword.length() > 32) {
                newPassword = RsaTool.decryptByPrivateKey(newPassword, privateKey);
            }
        }

        IamUserAuthPassword currentPwd = ssoLoginMapper.getPasswordByUserCode(userCode);
        if (currentPwd == null) {
            throw UserException.of("用户密码记录不存在");
        }

        if (!passwordEncoder.matches(oldPassword, currentPwd.getSalt(), currentPwd.getPassword())) {
            throw UserException.of("旧密码错误");
        }

        List<IamUserPasswordHis> historyList = ssoLoginMapper.getPasswordHisByUserCode(userCode, 3);
        for (IamUserPasswordHis his : historyList) {
            if (passwordEncoder.matches(newPassword, his.getSalt(), his.getPassword())) {
                throw UserException.of("新密码不能与最近3次使用过的密码相同");
            }
        }

        String encryptedPassword = passwordEncoder.encode(newPassword, null);
        String newSalt = "";

        IamUserAuthPassword updatePwd = new IamUserAuthPassword();
        updatePwd.setUserCode(userCode);
        updatePwd.setPassword(encryptedPassword);
        updatePwd.setSalt(newSalt);
        updatePwd.setVersion(currentPwd.getVersion());
        ssoLoginMapper.updatePasswordByUserCode(updatePwd);

        IamUserPasswordHis his = new IamUserPasswordHis();
        his.setUserCode(userCode);
        his.setPassword(encryptedPassword);
        his.setSalt(newSalt);
        his.setCreateBy(userCode);
        his.setUpdateBy(userCode);
        ssoLoginMapper.insertPasswordHis(his);

        // 修改密码成功，使该用户所有会话失效
        String username = SessionHelper.getUserJwt().getUsername();
        iamSessionService.invalidateAllSessions(username);
        log.info("用户 {} 修改密码成功，所有会话已失效", username);
    }

    // ========== 内部方法 ==========

    /**
     * 构建认证失败的响应，并发布 LoginFailedEvent。
     */
    private LoginResp failResp(LoginStatus loginStatus, String username, String clientIp, String userAgent) {
        eventPublisher.publishEvent(new LoginFailedEvent(username, clientIp, userAgent, loginStatus));

        LoginResp resp = new LoginResp();
        resp.setLoginStatus(loginStatus.getCode());
        resp.setLoginMessage(loginStatus.getMessage());
        return resp;
    }
}
