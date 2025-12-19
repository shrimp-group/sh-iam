package com.wkclz.iam.sso.service;

import com.alibaba.fastjson2.JSON;
import com.wkclz.iam.sdk.model.UserSession;
import com.wkclz.iam.sdk.service.IamSsoService;
import com.wkclz.iam.sdk.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class IamSsoServiceImpl implements IamSsoService {


    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public UserSession tokenCheck(String token, String username) {
        String tokenRedisKey = JwtUtil.getTokenRedisKey(token, username);
        String userSessionStr = stringRedisTemplate.opsForValue().get(tokenRedisKey);
        if (userSessionStr == null) {
            return null;
        }
        return JSON.parseObject(userSessionStr, UserSession.class);
    }
}
