package com.wkclz.iam.sso.service;

import com.wkclz.auth.contract.auth.CaptchaService;
import com.wkclz.auth.contract.auth.Md5PasswordEncoder;
import com.wkclz.auth.contract.auth.SessionStore;
import com.wkclz.core.exception.UserException;
import com.wkclz.iam.common.dto.IamUserAuthDto;
import com.wkclz.iam.common.entity.IamLoginLog;
import com.wkclz.iam.common.entity.IamUserAuth;
import com.wkclz.iam.common.entity.IamUserAuthPassword;
import com.wkclz.iam.common.entity.IamUserPasswordHis;
import com.wkclz.iam.sdk.contract.bean.req.SessionCreateReq;
import com.wkclz.iam.sdk.contract.bean.resp.LoginResp;
import com.wkclz.auth.context.SecurityContext;
import com.wkclz.auth.enums.AuthErrorType;
import com.wkclz.iam.sdk.contract.facade.SsoFacadeContract;
import com.wkclz.iam.sdk.bean.enums.AuthType;
import com.wkclz.iam.sdk.bean.enums.LoginStatus;
import com.wkclz.iam.sdk.bean.req.ChangePasswordReq;
import com.wkclz.iam.sdk.bean.req.LoginReq;
import com.wkclz.iam.sso.config.IamSsoConfig;
import com.wkclz.iam.sso.mapper.SsoLoginLogMapper;
import com.wkclz.iam.sso.mapper.SsoLoginMapper;
import com.wkclz.tool.tools.RsaTool;
import com.wkclz.tool.utils.SecretUtil;
import com.wkclz.web.helper.IpHelper;
import com.wkclz.web.helper.RequestHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class IamLoginService {

    private static final Logger log = LoggerFactory.getLogger(IamLoginService.class);

    @Autowired
    private SsoFacadeContract ssoFacadeContract;
    @Autowired
    private IamSsoConfig iamSsoConfig;
    @Autowired
    private SsoLoginMapper ssoLoginMapper;
    @Autowired
    private SsoLoginLogMapper ssoLoginLogMapper;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private SessionStore sessionStore;
    @Autowired
    private Md5PasswordEncoder passwordEncoder;

    /**
     * 1. 用户不存在
     * 2. 登录方式已禁用
     * 3. 用户已锁定
     * 4. 用户已禁用
     * 5. 密码错误
     * 6. 密码已过期
     * 7. 登录成功
     */

    public LoginResp loginByUsernameAndPassword(HttpServletRequest request, LoginReq loginReq) {
        String username = loginReq.getUsername();
        String captchaCode = loginReq.getCaptchaCode();
        String captchaId = loginReq.getCaptchaId();

        // 密码解密
        String password = loginReq.getPassword();
        String privateKey = iamSsoConfig.getPrivateKey();
        if (StringUtils.isNotBlank(privateKey) && password.length() > 32) {
            password = RsaTool.decryptByPrivateKey(password, privateKey);
        }

        IamUserAuthDto auth = ssoLoginMapper.getUserAuth4PasswordByUsername(username);

        // 0. 需要验证码
        IamLoginLog param = new IamLoginLog();
        param.setAuthIdentifier(username);
        param.setAuthType(AuthType.PASSWORD.name());
        IamLoginLog lastLoginIn1Hour = ssoLoginLogMapper.getLastLoginIn1Hour(param);
        if (lastLoginIn1Hour != null && lastLoginIn1Hour.getLoginStatus() != 0
                && (StringUtils.isBlank(captchaCode) || StringUtils.isBlank(captchaId))) {
            log.info("用户 {} 距离上次登录失败，在 1 小时内，需要验证码", username);
            loginLog(loginReq, auth, LoginStatus.NEED_CAPTCHA, AuthType.PASSWORD);
            return LoginResp.fail(AuthErrorType.CAPTCHA_REQUIRED);
        }

        // 验证码校验
        if (StringUtils.isNotBlank(captchaId)) {
            if (!captchaService.verify(captchaId, captchaCode)) {
                loginLog(loginReq, auth, LoginStatus.INVALID_CAPTCHA, AuthType.PASSWORD);
                return LoginResp.fail(AuthErrorType.CAPTCHA_ERROR);
            }
        }


        // 1. 用户不存在
        if (auth ==  null) {
            loginLog(loginReq, auth, LoginStatus.USER_NOT_FOUND, AuthType.PASSWORD);
            return LoginResp.fail(AuthErrorType.USERNAME_OR_PASSWORD_ERROR);
        }

        // 2. 登录方式已禁用
        if (auth.getAuthStatus().equals(0)) {
            loginLog(loginReq, auth, LoginStatus.EXPIRED_ACCOUNT, AuthType.PASSWORD);
            return LoginResp.fail(AuthErrorType.ACCOUNT_DISABLED);
        }

        // 3. 用户已锁定
        if (auth.getUserStatus().equals(3)) {
            loginLog(loginReq, auth, LoginStatus.ACCOUNT_LOCKED, AuthType.PASSWORD);
            return LoginResp.fail(AuthErrorType.ACCOUNT_LOCKED);
        }

        // 4. 用户已禁用
        if (auth.getUserStatus().equals(2)) {
            loginLog(loginReq, auth, LoginStatus.ACCOUNT_DISABLED, AuthType.PASSWORD);
            return LoginResp.fail(AuthErrorType.ACCOUNT_DISABLED);
        }

        // 5. 密码错误
        if (!passwordEncoder.matches(password, auth.getSalt(), auth.getPassword())) {
            loginLog(loginReq, auth, LoginStatus.INVALID_CREDENTIALS, AuthType.PASSWORD);
            return LoginResp.fail(AuthErrorType.USERNAME_OR_PASSWORD_ERROR);
        }

        // 6. 密码已过期
        Integer passwordExpireDays = iamSsoConfig.getPasswordExpireDays();

        ZonedDateTime zonedDateTime = auth.getLastChangedTime().atZone(ZoneId.systemDefault());
        long timestamp = zonedDateTime.toInstant().toEpochMilli();
        long passwordExpireAt = timestamp + passwordExpireDays * 24 * 60 * 60 * 1000L;
        if (passwordExpireAt < System.currentTimeMillis()) {
            loginLog(loginReq, auth, LoginStatus.EXPIRED_PASSWORD, AuthType.PASSWORD);
            return LoginResp.fail(AuthErrorType.CREDENTIALS_EXPIRED);
        }

        // 7. 登录成功，通过 SsoFacade 创建会话
        SessionCreateReq sessionCreateReq = new SessionCreateReq();
        sessionCreateReq.setUserCode(auth.getUserCode());
        sessionCreateReq.setUsername(auth.getUsername());
        sessionCreateReq.setAuthIdentifier(auth.getAuthIdentifier());
        sessionCreateReq.setNickname(auth.getNickname());
        sessionCreateReq.setAvatar(auth.getAvatar());
        sessionCreateReq.setAuthType(auth.getAuthType());
        log.info("用户 {} 认证成功，调用 SsoFacade 创建会话", auth.getAuthIdentifier());

        LoginResp response = ssoFacadeContract.login(sessionCreateReq);

        // 登录成功，需要更新的信息
        IamUserAuth userAuth = new IamUserAuth();
        userAuth.setId(auth.getId());
        userAuth.setLastLoginIp(IpHelper.getOriginIp(request));
        ssoLoginMapper.updateUserLoginInfo(userAuth);

        return response;
    }


    public void logout(HttpServletRequest request) {
        String token = SecurityContext.getToken();
        if (StringUtils.isBlank(token)) return;
        ssoFacadeContract.logout(token);
    }


    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordReq request) {
        Assert.notNull(request.getOldPassword(), "旧密码不能为空");
        Assert.notNull(request.getNewPassword(), "新密码不能为空");

        String userCode = SecurityContext.getUserCode();
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
        if (isPasswordInHistory(newPassword, historyList)) {
            throw UserException.of("新密码不能与最近3次使用过的密码相同");
        }

        String newSalt = SecretUtil.getKey();
        String encryptedPassword = passwordEncoder.encode(newPassword, newSalt);

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
        String username = SecurityContext.getUsername();
        sessionStore.deleteBySubjectId(username);
        log.info("用户 {} 修改密码成功，所有会话已失效", username);
    }


    private void loginLog(LoginReq loginReq, IamUserAuthDto auth, LoginStatus loginStatus, AuthType loginType) {
        IamLoginLog log = new IamLoginLog();
        log.setAuthIdentifier(loginReq.getUsername());
        log.setAuthType(loginType.name());
        log.setLoginStatus(loginStatus.getCode());
        log.setMessage(loginStatus.getMessage());
        log.setCreateBy(loginReq.getUsername());
        log.setUpdateBy(loginReq.getUsername());

        if (auth != null) {
            log.setUserCode(auth.getUserCode());
            log.setUsername(auth.getAuthIdentifier());
            log.setCreateBy(log.getUsername());
            log.setUpdateBy(log.getUsername());
        }

        HttpServletRequest request = RequestHelper.getRequest();
        if (request != null) {
            String originIp = IpHelper.getOriginIp(request);
            log.setIpAddress(originIp);
            log.setUserAgent(request.getHeader("User-Agent"));
        }
        ssoLoginLogMapper.insertLoginLog(log);
    }


    private boolean isPasswordInHistory(String newPassword, List<IamUserPasswordHis> historyList) {
        if (newPassword == null || historyList == null || historyList.isEmpty()) {
            return false;
        }
        for (IamUserPasswordHis his : historyList) {
            if (passwordEncoder.matches(newPassword, his.getSalt(), his.getPassword())) {
                return true;
            }
        }
        return false;
    }
}
