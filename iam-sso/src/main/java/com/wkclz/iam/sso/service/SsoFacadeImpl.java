package com.wkclz.iam.sso.service;

import com.alibaba.fastjson2.JSON;
import com.wkclz.iam.common.entity.IamLoginLog;
import com.wkclz.iam.sdk.bean.UserJwt;
import com.wkclz.iam.sdk.bean.UserSession;
import com.wkclz.iam.sdk.bean.enums.LoginStatus;
import com.wkclz.iam.sdk.bean.req.SessionCreateReq;
import com.wkclz.iam.sdk.bean.resp.LoginResp;
import com.wkclz.iam.sdk.config.IamSdkConfig;
import com.wkclz.iam.sdk.facade.SsoFacade;
import com.wkclz.iam.sdk.bean.RequestLog;
import com.wkclz.iam.sdk.helper.SessionHelper;
import com.wkclz.iam.sdk.util.JwtUtil;
import com.wkclz.iam.sso.config.IamSsoConfig;
import com.wkclz.iam.sso.mapper.SsoLoginLogMapper;
import com.wkclz.tool.tools.Md5Tool;
import com.wkclz.web.helper.IpHelper;
import com.wkclz.web.helper.RequestHelper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class SsoFacadeImpl implements SsoFacade {

    private static final Logger log = LoggerFactory.getLogger(SsoFacadeImpl.class);

    @Autowired
    private IamRequestService requestLogService;

    @Autowired
    private IamSessionService iamSessionService;

    @Autowired
    private IamSdkConfig iamSdkConfig;

    @Autowired
    private IamSsoConfig iamSsoConfig;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private SsoLoginLogMapper ssoLoginLogMapper;

    @Override
    public LoginResp login(SessionCreateReq req) {
        log.info("SsoFacade 本地创建会话, authIdentifier: {}", req.getAuthIdentifier());

        // 1. 生成 JWT
        UserJwt jwt = new UserJwt();
        jwt.setUserCode(req.getUserCode());
        jwt.setUsername(req.getUsername());
        jwt.setNickname(req.getNickname());
        jwt.setAvatar(req.getAvatar());
        String jwtToken = JwtUtil.generateToken(jwt, iamSdkConfig.getJwtSecretKey());

        // 2. 缓存 UserSession 到 Redis
        UserSession us = new UserSession();
        us.setUserCode(req.getUserCode());
        us.setUsername(req.getAuthIdentifier());
        us.setNickname(req.getNickname());
        us.setAuthType(req.getAuthType());

        String tokenRedisKey = JwtUtil.getTokenRedisKey(jwtToken, jwt.getUsername());
        redisTemplate.opsForValue().set(tokenRedisKey, JSON.toJSONString(us), JwtUtil.SESSION_TTL_SECONDS, TimeUnit.SECONDS);

        // 3. 注册会话到用户会话列表
        String sessionListKey = JwtUtil.getSessionListRedisKey(jwt.getUsername());
        String tokenMd5 = Md5Tool.md5(jwtToken);
        long currentTime = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(sessionListKey, tokenMd5, currentTime);
        log.info("用户 {} 会话已创建, tokenMd5={}", jwt.getUsername(), tokenMd5);

        // 4. 并发会话数控制
        Integer maxConcurrentSessions = iamSsoConfig.getMaxConcurrentSessions();
        if (maxConcurrentSessions != null && maxConcurrentSessions > 0) {
            Long sessionCount = redisTemplate.opsForZSet().size(sessionListKey);
            if (sessionCount != null && sessionCount > maxConcurrentSessions) {
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

        // 5. 记录登录日志
        IamLoginLog loginLog = new IamLoginLog();
        loginLog.setAuthIdentifier(req.getAuthIdentifier());
        loginLog.setAuthType(req.getAuthType());
        loginLog.setLoginStatus(LoginStatus.SUCCESS.getCode());
        loginLog.setMessage(LoginStatus.SUCCESS.getMessage());
        loginLog.setUserCode(req.getUserCode());
        loginLog.setUsername(req.getAuthIdentifier());
        loginLog.setCreateBy(req.getAuthIdentifier());
        loginLog.setUpdateBy(req.getAuthIdentifier());

        HttpServletRequest request = RequestHelper.getRequest();
        if (request != null) {
            loginLog.setIpAddress(IpHelper.getOriginIp(request));
            loginLog.setUserAgent(request.getHeader("User-Agent"));
        }
        ssoLoginLogMapper.insertLoginLog(loginLog);

        // 6. 返回结果
        LoginResp response = new LoginResp();
        response.setLoginStatus(LoginStatus.SUCCESS.getCode());
        response.setLoginMessage(LoginStatus.SUCCESS.getMessage());
        response.setToken(jwtToken);
        return response;
    }

    @Override
    public void saveLog(RequestLog log) {
        requestLogService.insertLog(log);
    }

    @Override
    public void logout(String token) {
        log.info("SsoFacade 本地登出, token: {}", token);
        iamSessionService.logout(token);
    }

    @Override
    public void logout() {
        String token = SessionHelper.getToken(RequestHelper.getRequest());
        if (StringUtils.isBlank(token)) {
            log.warn("SsoFacade 本地登出，当前请求上下文中 token 为空，跳过登出处理");
            return;
        }
        logout(token);
    }

}
