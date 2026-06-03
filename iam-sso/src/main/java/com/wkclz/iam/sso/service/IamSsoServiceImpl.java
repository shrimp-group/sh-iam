package com.wkclz.iam.sso.service;

import com.alibaba.fastjson2.JSON;
import com.wkclz.iam.sdk.model.UserSession;
import com.wkclz.iam.sdk.service.IamSsoService;
import com.wkclz.iam.sdk.util.JwtUtil;
import com.wkclz.tool.tools.Md5Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IamSsoServiceImpl implements IamSsoService {


    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public UserSession tokenCheck(String token, String username) {
        String tokenRedisKey = JwtUtil.getTokenRedisKey(token, username);
        String userSessionStr = stringRedisTemplate.opsForValue().get(tokenRedisKey);
        if (userSessionStr == null) {
            // Session 已不存在，清理会话列表中的幽灵条目
            String sessionListKey = JwtUtil.getSessionListRedisKey(username);
            String tokenMd5 = Md5Tool.md5(token);
            stringRedisTemplate.opsForZSet().remove(sessionListKey, tokenMd5);
            log.debug("Session 已过期，清理会话列表幽灵条目, user={}, tokenMd5={}", username, tokenMd5);
            return null;
        }

        // 校验 Token 是否仍在用户会话列表中
        String sessionListKey = JwtUtil.getSessionListRedisKey(username);
        String tokenMd5 = Md5Tool.md5(token);
        Double score = stringRedisTemplate.opsForZSet().score(sessionListKey, tokenMd5);
        if (score == null) {
            // Token 已被踢出，Session 也应删除
            stringRedisTemplate.delete(tokenRedisKey);
            log.info("会话已被踢出，删除 Session, user={}, tokenMd5={}", username, tokenMd5);
            return null;
        }
        return JSON.parseObject(userSessionStr, UserSession.class);
    }
}
