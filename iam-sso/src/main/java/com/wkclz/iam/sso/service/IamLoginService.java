package com.wkclz.iam.sso.service;

import com.alibaba.fastjson2.JSON;
import com.wkclz.iam.common.dto.IamUserAuthDto;
import com.wkclz.iam.common.entity.IamLoginLog;
import com.wkclz.iam.common.entity.IamUserAuth;
import com.wkclz.iam.sdk.config.IamSdkConfig;
import com.wkclz.iam.sdk.enums.LoginStatus;
import com.wkclz.iam.sdk.enums.LoginType;
import com.wkclz.iam.sdk.helper.CaptchaHelper;
import com.wkclz.iam.sdk.model.LoginRequest;
import com.wkclz.iam.sdk.model.LoginResponse;
import com.wkclz.iam.sdk.model.UserJwt;
import com.wkclz.iam.sdk.model.UserSession;
import com.wkclz.iam.sdk.util.JwtUtil;
import com.wkclz.iam.sso.config.IamSsoConfig;
import com.wkclz.iam.sso.mapper.SsoLoginLogMapper;
import com.wkclz.iam.sso.mapper.SsoLoginMapper;
import com.wkclz.web.helper.IpHelper;
import com.wkclz.web.helper.RequestHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class IamLoginService {

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

    /**
     * 1. 用户不存在
     * 2. 登录方式已禁用
     * 3. 用户已锁定
     * 4. 用户已禁用
     * 5. 密码错误
     * 6. 密码已过期
     * 7. 登录成功
     */

    public LoginResponse loginByUsernameAndPassword(HttpServletRequest request, LoginRequest loginRequest) {
        LoginResponse response = new LoginResponse();

        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        String captchaCode = loginRequest.getCaptchaCode();
        String captchaId = loginRequest.getCaptchaId();

        IamUserAuthDto auth = ssoLoginMapper.getUserAuth4PasswordByUsername(username);

        // 0. 需要验证码
        IamLoginLog param = new IamLoginLog();
        param.setAuthIdentifier(username);
        param.setLoginType(LoginType.PASSWORD.name());
        Integer loginFaildCountIn1Hour = ssoLoginLogMapper.getLoginFaildCountIn1Hour(param);
        if (loginFaildCountIn1Hour > 0 && (StringUtils.isBlank(captchaCode) || StringUtils.isBlank(captchaId))) {
            response.setLoginStatus(LoginStatus.NEED_CAPTCHA.getCode());
            response.setLoginMessage(LoginStatus.NEED_CAPTCHA.getMessage());
            loginLog(loginRequest, auth, LoginStatus.NEED_CAPTCHA, LoginType.PASSWORD);
            return response;
        }

        // 验证码校验
        if (StringUtils.isNotBlank(captchaId)) {
            String redisKey = CaptchaHelper.getCaptchaRedisKey(captchaId);
            String redisCaptchaCode = redisTemplate.opsForValue().get(redisKey);

            // 验证码不存在或已过期
            if (StringUtils.isBlank(redisCaptchaCode)) {
                response.setLoginStatus(LoginStatus.CAPTCHA_TIMEOUT.getCode());
                response.setLoginMessage(LoginStatus.CAPTCHA_TIMEOUT.getMessage());
                loginLog(loginRequest, auth, LoginStatus.CAPTCHA_TIMEOUT, LoginType.PASSWORD);
                return response;
            }
            // 验证码错误
            if (!captchaCode.equals(redisCaptchaCode)) {
                response.setLoginStatus(LoginStatus.INVALID_CAPTCHA.getCode());
                response.setLoginMessage(LoginStatus.INVALID_CAPTCHA.getMessage());
                loginLog(loginRequest, auth, LoginStatus.INVALID_CAPTCHA, LoginType.PASSWORD);
                return response;
            }
        }


        // 1. 用户不存在
        if (auth ==  null) {
            response.setLoginStatus(LoginStatus.USER_NOT_FOUND.getCode());
            response.setLoginMessage("用户不存在, 或密码错误!");
            loginLog(loginRequest, auth, LoginStatus.USER_NOT_FOUND, LoginType.PASSWORD);
            return response;
        }

        // 2. 登录方式已禁用
        if (auth.getAuthStatus().equals(0)) {
            response.setLoginStatus(LoginStatus.EXPIRED_ACCOUNT.getCode());
            response.setLoginMessage(LoginStatus.EXPIRED_ACCOUNT.getMessage());
            loginLog(loginRequest, auth, LoginStatus.EXPIRED_ACCOUNT, LoginType.PASSWORD);
            return response;
        }

        // 3. 用户已锁定
        if (auth.getUserStatus().equals(3)) {
            response.setLoginStatus(LoginStatus.ACCOUNT_LOCKED.getCode());
            response.setLoginMessage(LoginStatus.ACCOUNT_LOCKED.getMessage());
            loginLog(loginRequest, auth, LoginStatus.ACCOUNT_LOCKED, LoginType.PASSWORD);
            return response;
        }

        // 4. 用户已禁用
        if (auth.getUserStatus().equals(2)) {
            response.setLoginStatus(LoginStatus.ACCOUNT_DISABLED.getCode());
            response.setLoginMessage(LoginStatus.ACCOUNT_DISABLED.getMessage());
            loginLog(loginRequest, auth, LoginStatus.ACCOUNT_DISABLED, LoginType.PASSWORD);
            return response;
        }

        // 5. 密码错误
        if (!auth.getPassword().equals(password)) {
            response.setLoginStatus(LoginStatus.INVALID_CREDENTIALS.getCode());
            response.setLoginMessage(LoginStatus.INVALID_CREDENTIALS.getMessage());
            loginLog(loginRequest, auth, LoginStatus.INVALID_CREDENTIALS, LoginType.PASSWORD);
            return response;
        }

        // 6. 密码已过期
        Integer passwordExpireDays = iamSsoConfig.getPasswordExpireDays();

        ZonedDateTime zonedDateTime = auth.getLastChangedTime().atZone(ZoneId.systemDefault());
        long timestamp = zonedDateTime.toInstant().toEpochMilli();
        long passwordExpireAt = timestamp + passwordExpireDays + 24 * 60 * 60 * 1000L;
        if (passwordExpireAt < System.currentTimeMillis()) {
            response.setLoginStatus(LoginStatus.EXPIRED_PASSWORD.getCode());
            response.setLoginMessage(LoginStatus.EXPIRED_PASSWORD.getMessage());
            loginLog(loginRequest, auth, LoginStatus.EXPIRED_PASSWORD, LoginType.PASSWORD);
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
        redisTemplate.opsForValue().set(tokenRedisKey, JSON.toJSONString(us));

        response.setLoginStatus(LoginStatus.SUCCESS.getCode());
        response.setLoginMessage(LoginStatus.SUCCESS.getMessage());
        response.setToken(jwtToken);
        loginLog(loginRequest, auth, LoginStatus.SUCCESS, LoginType.PASSWORD);

        // 登录成功，需要更新的信息
        IamUserAuth userAuth = new IamUserAuth();
        userAuth.setId(auth.getId());
        userAuth.setLastLoginIp(IpHelper.getOriginIp(request));
        ssoLoginMapper.updateUserLoginInfo(userAuth);

        return response;
    }



    private void loginLog(LoginRequest loginRequest, IamUserAuthDto auth, LoginStatus loginStatus, LoginType loginType) {
        IamLoginLog log = new IamLoginLog();
        log.setAuthIdentifier(loginRequest.getUsername());
        log.setLoginType(loginType.name());
        log.setLoginStatus(loginStatus.getCode());
        log.setMessage(loginStatus.getMessage());
        log.setCreateBy(loginRequest.getUsername());
        log.setUpdateBy(loginRequest.getUsername());

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
