package com.wkclz.iam.sso.service;

import com.wkclz.core.exception.UserException;
import com.wkclz.core.identity.IdentityContext;
import com.wkclz.core.identity.UserIdentity;
import com.wkclz.iam.common.entity.IamLoginRecord;
import com.wkclz.iam.common.entity.IamUserAuth;
import com.wkclz.iam.common.entity.IamUserAuthPassword;
import com.wkclz.iam.common.entity.IamUserPasswordHis;
import com.wkclz.iam.session.bean.SessionCreateResult;
import com.wkclz.iam.session.bean.resp.LoginResp;
import com.wkclz.iam.session.enums.AuthType;
import com.wkclz.iam.session.enums.DestroyReason;
import com.wkclz.iam.session.enums.LoginStatus;
import com.wkclz.iam.session.service.SessionManager;
import com.wkclz.iam.sso.bean.req.ChangePasswordReq;
import com.wkclz.iam.sso.bean.req.LoginReq;
import com.wkclz.iam.sso.config.IamSsoConfig;
import com.wkclz.iam.sso.event.LoginEvent;
import com.wkclz.iam.sso.event.LogoutEvent;
import com.wkclz.iam.sso.event.PasswordChangedEvent;
import com.wkclz.iam.sso.mapper.SsoLoginMapper;
import com.wkclz.iam.sso.mapper.SsoLoginRecordMapper;
import com.wkclz.iam.sso.spi.CredentialChecker;
import com.wkclz.iam.sso.spi.PasswordEncoder;
import com.wkclz.tool.tools.Md5Tool;
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
    private IamSsoConfig iamSsoConfig;
    @Autowired
    private SsoLoginMapper ssoLoginMapper;
    @Autowired
    private SsoLoginRecordMapper ssoLoginRecordMapper;
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
        IamLoginRecord param = new IamLoginRecord();
        param.setAuthIdentifier(username);
        param.setAuthType(AuthType.PASSWORD.name());
        IamLoginRecord lastLoginIn1Hour = ssoLoginRecordMapper.getLastLoginIn1Hour(param);
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
        eventPublisher.publishEvent(LoginEvent.success(username, userIdentity.getUserCode(), clientIp, userAgent, sessionResult.getToken()));

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

    /**
     * 登出。
     *
     * <p>从 IdentityContext 取当前 token → SessionManager.destroySession → 发布 LogoutEvent → 清理上下文。</p>
     */
    public void logout() {
        String token = IdentityContext.getToken();
        if (StringUtils.isBlank(token)) {
            log.debug("未登录状态调用登出，跳过");
            return;
        }
        String username = IdentityContext.getUsername();
        log.info("用户 {} 登出", username);

        // 委托 SessionManager 销毁会话
        String sessionId = Md5Tool.md5(token);
        sessionManager.destroySession(sessionId, DestroyReason.LOGOUT);

        // 发布登出事件
        eventPublisher.publishEvent(new LogoutEvent(username, token));

        // 清理身份上下文
        IdentityContext.clear();
    }

    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordReq request) {
        Assert.notNull(request.getOldPassword(), "旧密码不能为空");
        Assert.notNull(request.getNewPassword(), "新密码不能为空");

        // 从 IdentityContext 获取当前用户
        UserIdentity userIdentity = IdentityContext.get();
        Assert.notNull(userIdentity, "当前用户未登录");
        String userCode = userIdentity.getUserCode();
        log.info("用户 {} 开始修改密码", userCode);

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

        // 旧密码校验
        IamUserAuthPassword currentPwd = ssoLoginMapper.getPasswordByUserCode(userCode);
        if (currentPwd == null) {
            throw UserException.of("用户密码记录不存在");
        }
        if (!passwordEncoder.matches(oldPassword, currentPwd.getSalt(), currentPwd.getPassword())) {
            throw UserException.of("旧密码错误");
        }

        // 历史密码检查（可配置，默认 5）
        int historySize = iamSsoConfig.getPasswordHistorySize() != null ? iamSsoConfig.getPasswordHistorySize() : 5;
        List<IamUserPasswordHis> historyList = ssoLoginMapper.getPasswordHisByUserCode(userCode, historySize);
        for (IamUserPasswordHis his : historyList) {
            if (passwordEncoder.matches(newPassword, his.getSalt(), his.getPassword())) {
                throw UserException.of("新密码不能与最近" + historySize + "次使用过的密码相同");
            }
        }

        // 编码新密码
        String encryptedPassword = passwordEncoder.encode(newPassword, null);
        String newSalt = "";

        IamUserAuthPassword updatePwd = new IamUserAuthPassword();
        updatePwd.setUserCode(userCode);
        updatePwd.setPassword(encryptedPassword);
        updatePwd.setSalt(newSalt);
        updatePwd.setVersion(currentPwd.getVersion());
        ssoLoginMapper.updatePasswordByUserCode(updatePwd);

        // 记录密码变更历史
        IamUserPasswordHis his = new IamUserPasswordHis();
        his.setUserCode(userCode);
        his.setPassword(encryptedPassword);
        his.setSalt(newSalt);
        his.setCreateBy(userCode);
        his.setUpdateBy(userCode);
        ssoLoginMapper.insertPasswordHis(his);

        // 全会话失效
        sessionManager.destroyAllSessions(userCode, DestroyReason.PASSWORD_CHANGED);
        log.info("用户 {} 修改密码成功，所有会话已失效", userCode);

        // 发布事件
        eventPublisher.publishEvent(new PasswordChangedEvent(userCode));
    }

    // ========== 内部方法 ==========

    /**
     * 构建认证失败的响应，并发布 LoginFailedEvent。
     */
    private LoginResp failResp(LoginStatus loginStatus, String username, String clientIp, String userAgent) {
        eventPublisher.publishEvent(LoginEvent.failed(username, clientIp, userAgent, loginStatus));

        LoginResp resp = new LoginResp();
        resp.setLoginStatus(loginStatus.getCode());
        resp.setLoginMessage(loginStatus.getMessage());
        return resp;
    }
}
