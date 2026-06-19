package com.wkclz.iam.sso.service;

import com.alibaba.fastjson2.JSON;
import com.wkclz.core.exception.UserException;
import com.wkclz.iam.common.dto.IamUserAuthDto;
import com.wkclz.iam.common.entity.IamLoginLog;
import com.wkclz.iam.common.entity.IamUserAuth;
import com.wkclz.iam.common.entity.IamUserAuthPassword;
import com.wkclz.iam.common.entity.IamUserPasswordHis;
import com.wkclz.iam.common.helper.PasswordHelper;
import com.wkclz.iam.sdk.config.IamSdkConfig;
import com.wkclz.iam.sdk.bean.enums.AuthType;
import com.wkclz.iam.sdk.bean.enums.LoginStatus;
import com.wkclz.iam.sdk.helper.CaptchaHelper;
import com.wkclz.iam.sdk.helper.SessionHelper;
import com.wkclz.iam.sdk.bean.req.ChangePasswordReq;
import com.wkclz.iam.sdk.bean.req.LoginReq;
import com.wkclz.iam.sdk.bean.resp.LoginResp;
import com.wkclz.iam.sdk.bean.UserJwt;
import com.wkclz.iam.sdk.bean.UserSession;
import com.wkclz.iam.sdk.util.JwtUtil;
import com.wkclz.iam.sso.config.IamSsoConfig;
import com.wkclz.iam.sso.mapper.SsoLoginLogMapper;
import com.wkclz.iam.sso.mapper.SsoLoginMapper;
import com.wkclz.tool.tools.Md5Tool;
import com.wkclz.tool.tools.RsaTool;
import com.wkclz.tool.utils.SecretUtil;
import com.wkclz.web.helper.IpHelper;
import com.wkclz.web.helper.RequestHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class IamLoginService {

    private static final Logger log = LoggerFactory.getLogger(IamLoginService.class);

    @Autowired
    private IamSsoConfig iamSsoConfig;
    @Autowired
    private IamSdkConfig iamSdkConfig;
    @Autowired
    private SsoLoginMapper ssoLoginMapper;
    @Autowired
    private SsoLoginLogMapper ssoLoginLogMapper;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private IamSessionService iamSessionService;

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
        LoginResp response = new LoginResp();

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
        IamLoginLog lastLoginIn1Hour = ssoLoginLogMapper.getLoginFaildCountIn1Hour(param);
        if (lastLoginIn1Hour != null && lastLoginIn1Hour.getLoginStatus() != 0
                && (StringUtils.isBlank(captchaCode) || StringUtils.isBlank(captchaId))) {
            response.setLoginStatus(LoginStatus.NEED_CAPTCHA.getCode());
            response.setLoginMessage(LoginStatus.NEED_CAPTCHA.getMessage());
            loginLog(loginReq, auth, LoginStatus.NEED_CAPTCHA, AuthType.PASSWORD);
            return response;
        }

        // 验证码校验
        if (StringUtils.isNotBlank(captchaId)) {
            String redisKey = CaptchaHelper.getCaptchaRedisKey(captchaId);
            String redisCaptchaCode = redisTemplate.opsForValue().getAndDelete(redisKey);

            // 验证码不存在或已过期
            if (StringUtils.isBlank(redisCaptchaCode)) {
                response.setLoginStatus(LoginStatus.CAPTCHA_TIMEOUT.getCode());
                response.setLoginMessage(LoginStatus.CAPTCHA_TIMEOUT.getMessage());
                loginLog(loginReq, auth, LoginStatus.CAPTCHA_TIMEOUT, AuthType.PASSWORD);
                return response;
            }
            // 验证码错误
            if (!captchaCode.equalsIgnoreCase(redisCaptchaCode)) {
                response.setLoginStatus(LoginStatus.INVALID_CAPTCHA.getCode());
                response.setLoginMessage(LoginStatus.INVALID_CAPTCHA.getMessage());
                loginLog(loginReq, auth, LoginStatus.INVALID_CAPTCHA, AuthType.PASSWORD);
                return response;
            }
        }


        // 1. 用户不存在
        if (auth ==  null) {
            response.setLoginStatus(LoginStatus.USER_NOT_FOUND.getCode());
            response.setLoginMessage("用户不存在, 或密码错误!");
            loginLog(loginReq, auth, LoginStatus.USER_NOT_FOUND, AuthType.PASSWORD);
            return response;
        }

        // 2. 登录方式已禁用
        if (auth.getAuthStatus().equals(0)) {
            response.setLoginStatus(LoginStatus.EXPIRED_ACCOUNT.getCode());
            response.setLoginMessage(LoginStatus.EXPIRED_ACCOUNT.getMessage());
            loginLog(loginReq, auth, LoginStatus.EXPIRED_ACCOUNT, AuthType.PASSWORD);
            return response;
        }

        // 3. 用户已锁定
        if (auth.getUserStatus().equals(3)) {
            response.setLoginStatus(LoginStatus.ACCOUNT_LOCKED.getCode());
            response.setLoginMessage(LoginStatus.ACCOUNT_LOCKED.getMessage());
            loginLog(loginReq, auth, LoginStatus.ACCOUNT_LOCKED, AuthType.PASSWORD);
            return response;
        }

        // 4. 用户已禁用
        if (auth.getUserStatus().equals(2)) {
            response.setLoginStatus(LoginStatus.ACCOUNT_DISABLED.getCode());
            response.setLoginMessage(LoginStatus.ACCOUNT_DISABLED.getMessage());
            loginLog(loginReq, auth, LoginStatus.ACCOUNT_DISABLED, AuthType.PASSWORD);
            return response;
        }

        // 5. 密码错误
        if (!PasswordHelper.validatePassword(password, auth.getSalt(), auth.getPassword())) {
            response.setLoginStatus(LoginStatus.INVALID_CREDENTIALS.getCode());
            response.setLoginMessage(LoginStatus.INVALID_CREDENTIALS.getMessage());
            loginLog(loginReq, auth, LoginStatus.INVALID_CREDENTIALS, AuthType.PASSWORD);
            return response;
        }

        // 6. 密码已过期
        Integer passwordExpireDays = iamSsoConfig.getPasswordExpireDays();

        ZonedDateTime zonedDateTime = auth.getLastChangedTime().atZone(ZoneId.systemDefault());
        long timestamp = zonedDateTime.toInstant().toEpochMilli();
        long passwordExpireAt = timestamp + passwordExpireDays * 24 * 60 * 60 * 1000L;
        if (passwordExpireAt < System.currentTimeMillis()) {
            response.setLoginStatus(LoginStatus.EXPIRED_PASSWORD.getCode());
            response.setLoginMessage(LoginStatus.EXPIRED_PASSWORD.getMessage());
            loginLog(loginReq, auth, LoginStatus.EXPIRED_PASSWORD, AuthType.PASSWORD);
            return response;
        }

        // 7. 登录成功

        // JWT, 生成 token 返回给前端
        UserJwt jwt = new UserJwt();
        jwt.setUserCode(auth.getUserCode());
        jwt.setUsername(auth.getUsername());
        jwt.setNickname(auth.getNickname());
        jwt.setAvatar(auth.getAvatar());
        String jwtToken = JwtUtil.generateToken(jwt, iamSdkConfig.getJwtSecretKey());

        // 用户信息，缓存到 Redis
        UserSession us = new UserSession();
        us.setUserCode(auth.getUserCode());
        us.setUsername(auth.getAuthIdentifier());
        us.setNickname(auth.getNickname());
        us.setAuthType(auth.getAuthType());

        String tokenRedisKey = JwtUtil.getTokenRedisKey(jwtToken, jwt.getUsername());
        redisTemplate.opsForValue().set(tokenRedisKey, JSON.toJSONString(us), JwtUtil.SESSION_TTL_SECONDS, TimeUnit.SECONDS);

        // 注册会话到用户会话列表
        String sessionListKey = JwtUtil.getSessionListRedisKey(jwt.getUsername());
        String tokenMd5 = Md5Tool.md5(jwtToken);
        long currentTime = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(sessionListKey, tokenMd5, currentTime);
        log.info("用户 {} 登录成功，会话已注册, tokenMd5={}", jwt.getUsername(), tokenMd5);

        // 并发会话数控制
        Integer maxConcurrentSessions = iamSsoConfig.getMaxConcurrentSessions();
        if (maxConcurrentSessions != null && maxConcurrentSessions > 0) {
            Long sessionCount = redisTemplate.opsForZSet().size(sessionListKey);
            if (sessionCount != null && sessionCount > maxConcurrentSessions) {
                // 按 score 升序取出最早登录的会话，踢出
                Set<String> earliestTokens = redisTemplate.opsForZSet().range(sessionListKey, 0, sessionCount - maxConcurrentSessions - 1);
                if (earliestTokens != null) {
                    for (String kickedTokenMd5 : earliestTokens) {
                        String kickedSessionKey = "iam:session:" + jwt.getUsername() + ":" + kickedTokenMd5;
                        redisTemplate.delete(kickedSessionKey);
                        redisTemplate.opsForZSet().remove(sessionListKey, kickedTokenMd5);
                        log.info("用户 {} 并发会话超限，踢出最早会话, tokenMd5={}", jwt.getUsername(), kickedTokenMd5);
                    }
                }
            }
        }

        response.setLoginStatus(LoginStatus.SUCCESS.getCode());
        response.setLoginMessage(LoginStatus.SUCCESS.getMessage());
        response.setToken(jwtToken);
        loginLog(loginReq, auth, LoginStatus.SUCCESS, AuthType.PASSWORD);

        // 登录成功，需要更新的信息
        IamUserAuth userAuth = new IamUserAuth();
        userAuth.setId(auth.getId());
        userAuth.setLastLoginIp(IpHelper.getOriginIp(request));
        ssoLoginMapper.updateUserLoginInfo(userAuth);

        return response;
    }


    public void logout(HttpServletRequest request) {
        String token = SessionHelper.getToken(request);
        if (StringUtils.isBlank(token)) {
            return;
        }
        UserSession userSession = SessionHelper.getUserSession(request);
        if (userSession == null) {
            return;
        }

        String tokenRedisKey = JwtUtil.getTokenRedisKey(token, userSession.getUsername());
        redisTemplate.opsForValue().getAndDelete(tokenRedisKey);

        // 从用户会话列表中移除当前 Token
        String sessionListKey = JwtUtil.getSessionListRedisKey(userSession.getUsername());
        String tokenMd5 = Md5Tool.md5(token);
        redisTemplate.opsForZSet().remove(sessionListKey, tokenMd5);
        log.info("用户 {} 登出，会话已移除, tokenMd5={}", userSession.getUsername(), tokenMd5);
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

        if (!PasswordHelper.validatePassword(oldPassword, currentPwd.getSalt(), currentPwd.getPassword())) {
            throw UserException.of("旧密码错误");
        }

        List<IamUserPasswordHis> historyList = ssoLoginMapper.getPasswordHisByUserCode(userCode, 3);
        if (PasswordHelper.isPasswordInHistory(newPassword, historyList)) {
            throw UserException.of("新密码不能与最近3次使用过的密码相同");
        }

        String newSalt = SecretUtil.getKey();
        String encryptedPassword = PasswordHelper.generatePassword(newPassword, newSalt);

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


}
