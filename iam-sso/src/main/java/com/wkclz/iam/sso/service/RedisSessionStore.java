package com.wkclz.iam.sso.service;

import com.alibaba.fastjson2.JSON;
import com.wkclz.auth.bean.Session;
import com.wkclz.auth.contract.auth.SessionStore;
import com.wkclz.tool.tools.Md5Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RedisSessionStore implements SessionStore {

    private static final long DEFAULT_TTL_SECONDS = 24 * 60 * 60L;
    private static final String SESSION_KEY_PREFIX = "sh-auth:session:";
    private static final String TOKEN_OWNER_KEY_PREFIX = "sh-auth:token:owner:";
    private static final String SESSION_LIST_KEY_PREFIX = "sh-auth:session:list:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public void save(Session session) {
        String tokenMd5 = Md5Tool.md5(session.getSessionId());
        String sessionKey = SESSION_KEY_PREFIX + tokenMd5;
        String tokenOwnerKey = TOKEN_OWNER_KEY_PREFIX + tokenMd5;
        String listKey = SESSION_LIST_KEY_PREFIX + session.getSubjectId();

        String sessionJson = JSON.toJSONString(session);
        redisTemplate.opsForValue().set(sessionKey, sessionJson, DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set(tokenOwnerKey, session.getSubjectId(), DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
        redisTemplate.opsForZSet().add(listKey, tokenMd5, System.currentTimeMillis());
    }

    @Override
    public Session get(String sessionId) {
        String tokenMd5 = Md5Tool.md5(sessionId);
        String ownerKey = TOKEN_OWNER_KEY_PREFIX + tokenMd5;
        String owner = redisTemplate.opsForValue().get(ownerKey);
        if (owner == null) {
            return null;
        }
        String sessionKey = SESSION_KEY_PREFIX + tokenMd5;
        String sessionJson = redisTemplate.opsForValue().get(sessionKey);
        if (sessionJson == null) {
            return null;
        }
        try {
            return JSON.parseObject(sessionJson, Session.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void delete(String sessionId) {
        String tokenMd5 = Md5Tool.md5(sessionId);
        String sessionKey = SESSION_KEY_PREFIX + tokenMd5;
        String ownerKey = TOKEN_OWNER_KEY_PREFIX + tokenMd5;

        String owner = redisTemplate.opsForValue().get(ownerKey);
        redisTemplate.delete(sessionKey);
        redisTemplate.delete(ownerKey);
        if (owner != null) {
            String listKey = SESSION_LIST_KEY_PREFIX + owner;
            redisTemplate.opsForZSet().remove(listKey, tokenMd5);
        }
    }

    @Override
    public void deleteBySubjectId(String subjectId) {
        String listKey = SESSION_LIST_KEY_PREFIX + subjectId;
        Set<String> tokenMd5Set = redisTemplate.opsForZSet().range(listKey, 0, -1);
        if (tokenMd5Set != null && !tokenMd5Set.isEmpty()) {
            List<String> keys = new ArrayList<>(tokenMd5Set.size() * 2);
            for (String tokenMd5 : tokenMd5Set) {
                keys.add(SESSION_KEY_PREFIX + tokenMd5);
                keys.add(TOKEN_OWNER_KEY_PREFIX + tokenMd5);
            }
            redisTemplate.delete(keys);
        }
        redisTemplate.delete(listKey);
    }

    @Override
    public void refresh(String sessionId, long ttlSeconds) {
        String tokenMd5 = Md5Tool.md5(sessionId);
        String sessionKey = SESSION_KEY_PREFIX + tokenMd5;
        String ownerKey = TOKEN_OWNER_KEY_PREFIX + tokenMd5;
        redisTemplate.expire(sessionKey, ttlSeconds, TimeUnit.SECONDS);
        redisTemplate.expire(ownerKey, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public List<Session> getActiveSessions(String subjectId) {
        String listKey = SESSION_LIST_KEY_PREFIX + subjectId;
        Set<String> tokenMd5Set = redisTemplate.opsForZSet().range(listKey, 0, -1);
        if (tokenMd5Set == null || tokenMd5Set.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> sessionKeys = new ArrayList<>(tokenMd5Set.size());
        for (String tokenMd5 : tokenMd5Set) {
            sessionKeys.add(SESSION_KEY_PREFIX + tokenMd5);
        }
        List<String> sessionJsons = redisTemplate.opsForValue().multiGet(sessionKeys);
        if (sessionJsons == null) {
            return new ArrayList<>();
        }
        List<Session> sessions = new ArrayList<>();
        for (String json : sessionJsons) {
            if (json != null) {
                try {
                    sessions.add(JSON.parseObject(json, Session.class));
                } catch (Exception ignored) {
                }
            }
        }
        return sessions;
    }
}
