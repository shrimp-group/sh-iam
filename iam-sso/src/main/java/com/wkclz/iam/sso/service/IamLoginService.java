package com.wkclz.iam.sso.service;

import com.wkclz.auth.bean.*;
import com.wkclz.auth.contract.auth.LoginService;
import com.wkclz.auth.contract.auth.PasswordEncoder;
import com.wkclz.auth.contract.auth.SessionStore;
import com.wkclz.auth.context.SecurityContext;
import com.wkclz.auth.enums.AuthErrorType;
import com.wkclz.auth.enums.CredentialType;
import com.wkclz.core.exception.UserException;
import com.wkclz.iam.common.entity.IamLoginLog;
import com.wkclz.iam.common.entity.IamUserAuthPassword;
import com.wkclz.iam.common.entity.IamUserPasswordHis;
import com.wkclz.iam.sso.bean.req.ChangePasswordReq;
import com.wkclz.iam.sso.bean.req.LoginReq;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * IAM 登录服务 — 处理 SSO 专属逻辑（RSA 解密、验证码需求判断、登录 IP 更新），
 * 标准认证流程委托 sh-auth {@link LoginService} 处理。
 *
 * @author shrimp
 */
@Service
public class IamLoginService {

    private static final Logger log = LoggerFactory.getLogger(IamLoginService.class);

    @Autowired
    private LoginService loginService;
    @Autowired
    private IamSsoConfig iamSsoConfig;
    @Autowired
    private SsoLoginMapper ssoLoginMapper;
    @Autowired
    private SsoLoginLogMapper ssoLoginLogMapper;
    @Autowired
    private SessionStore sessionStore;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 用户名密码登录 — SSO 专属处理 + 标准认证委托
     * <p>
     * 流程：
     * <ol>
     *   <li>RSA 密码解密（SSO 专属）</li>
     *   <li>1h 失败历史 → 需验证码判断（SSO 专属）</li>
     *   <li>构建 AuthRequest → 委托 sh-auth LoginService.login()</li>
     *   <li>登录成功 → 更新最后登录 IP（SSO 专属）</li>
     *   <li>转换 AuthResult → LoginResp</li>
     * </ol>
     * </p>
     */
    public LoginResp loginByUsernameAndPassword(HttpServletRequest request, LoginReq loginReq) {
        String username = loginReq.getUsername();
        String captchaCode = loginReq.getCaptchaCode();
        String captchaId = loginReq.getCaptchaId();

        // SSO 专属: RSA 密码解密
        String password = loginReq.getPassword();
        String privateKey = iamSsoConfig.getPrivateKey();
        if (StringUtils.isNotBlank(privateKey) && password != null && password.length() > 32) {
            password = RsaTool.decryptByPrivateKey(password, privateKey);
        }

        // SSO 专属: 1h 失败历史 → 需要验证码
        IamLoginLog param = new IamLoginLog();
        param.setAuthIdentifier(username);
        param.setAuthType(CredentialType.PASSWORD.name());
        IamLoginLog lastLogin = ssoLoginLogMapper.getLastLoginIn1Hour(param);
        if (lastLogin != null && lastLogin.getLoginStatus() != null && lastLogin.getLoginStatus() != 0
            && (StringUtils.isBlank(captchaCode) || StringUtils.isBlank(captchaId))) {
            log.info("用户 {} 距离上次登录失败，在 1 小时内，需要验证码", username);
            recordCaptchaRequiredLog(loginReq);
            return LoginResp.fail(AuthErrorType.CAPTCHA_REQUIRED);
        }

        // 构建标准 AuthRequest → 委托 sh-auth 标准登录流程
        AuthRequest authRequest = buildAuthRequest(loginReq, password);
        log.info("用户 {} 开始标准登录流程", username);

        AuthResult result = loginService.login(authRequest, request);

        // SSO 专属: 登录成功 → 更新最后登录 IP
        if (result.isSuccess()) {
            Principal principal = result.getPrincipal();
            ssoLoginMapper.updateUserLoginInfoByUserCode(principal.getUserCode(), IpHelper.getOriginIp(request));
            log.info("用户 {} 登录成功，已更新最后登录 IP", principal.getUsername());
        }

        return toLoginResp(result);
    }

    public void logout(HttpServletRequest request) {
        String token = SecurityContext.getToken();
        if (StringUtils.isBlank(token)) return;
        sessionStore.delete(token);
        log.info("用户已登出, token={}...", token.substring(0, Math.min(8, token.length())));
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

        String username = SecurityContext.getUsername();
        sessionStore.deleteBySubjectId(username);
        log.info("用户 {} 修改密码成功，所有会话已失效", username);
    }

    private AuthRequest buildAuthRequest(LoginReq loginReq, String decryptedPassword) {
        AuthRequest authRequest = new AuthRequest();
        authRequest.setAuthType("PASSWORD");

        Credential credential = new Credential();
        credential.setType(CredentialType.PASSWORD);
        credential.setCredentialValue(decryptedPassword);
        if (StringUtils.isNotBlank(loginReq.getCaptchaId())) {
            credential.setCaptchaId(loginReq.getCaptchaId());
            credential.setCaptchaCode(loginReq.getCaptchaCode());
        }
        authRequest.setCredential(credential);

        Map<String, Object> extra = new HashMap<>();
        extra.put("username", loginReq.getUsername());
        authRequest.setExtra(extra);

        return authRequest;
    }

    private LoginResp toLoginResp(AuthResult result) {
        if (result.isSuccess()) {
            Principal principal = result.getPrincipal();
            return LoginResp.success(
                result.getToken().getTokenValue(),
                principal.getUserCode(),
                principal.getUsername(),
                principal.getNickname(),
                principal.getAvatar()
            );
        }
        return LoginResp.fail(result.getErrorType(), result.getErrorMessage());
    }

    private void recordCaptchaRequiredLog(LoginReq loginReq) {
        IamLoginLog log = new IamLoginLog();
        log.setAuthIdentifier(loginReq.getUsername());
        log.setAuthType(CredentialType.PASSWORD.name());
        log.setLoginStatus(AuthErrorType.CAPTCHA_REQUIRED.getCode());
        log.setMessage(AuthErrorType.CAPTCHA_REQUIRED.getMessage());
        HttpServletRequest request = RequestHelper.getRequest();
        if (request != null) {
            log.setIpAddress(IpHelper.getOriginIp(request));
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
