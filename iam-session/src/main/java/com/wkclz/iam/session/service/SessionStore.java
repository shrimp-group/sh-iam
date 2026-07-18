package com.wkclz.iam.session.service;

import com.wkclz.iam.session.bean.Session;
import com.wkclz.iam.session.enums.AuthType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话持久化 — 具体类，基于 Redis Hash + ZSet。
 *
 * <p>sessionId = MD5(token)，查找时对 token 做 MD5 定位 Redis Key。
 * 不设接口，仅有 Redis 一个实现。若未来需要其他后端，届时再抽象。</p>
 */
@Component
public class SessionStore {

    /**
     * Redis Key 前缀
     */
    public static final String KEY_PREFIX = "iam:session:";
    /**
     * 用户会话索引 Key 前缀
     */
    public static final String INDEX_PREFIX = "iam:session:index:";

    private final StringRedisTemplate redisTemplate;

    public SessionStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 保存会话。
     * <p>putAll 一次性 HSET + Pipeline 批量 EXPIRE + ZADD，共 3 次 Redis 调用。</p>
     */
    public void save(Session session, long ttlSeconds) {
        String key = KEY_PREFIX + session.getSessionId();

        // 一次 putAll 设置全部字段
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("sessionId", session.getSessionId());
        fields.put("subjectId", session.getSubjectId());
        fields.put("authType", session.getAuthType().name());
        if (session.getToken() != null) {
            fields.put("token", session.getToken());
        }
        if (session.getUserIdentity() != null) {
            fields.put("userIdentity", session.getUserIdentity());
        }
        fields.put("createTime", String.valueOf(session.getCreateTime()));
        fields.put("expireTime", String.valueOf(session.getExpireTime()));
        redisTemplate.opsForHash().putAll(key, fields);

        // Pipeline: EXPIRE + ZADD 批量执行
        String indexKey = INDEX_PREFIX + session.getSubjectId();
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            connection.keyCommands().expire(key.getBytes(), ttlSeconds);
            connection.zSetCommands().zAdd(
                indexKey.getBytes(),
                session.getCreateTime(),
                session.getSessionId().getBytes()
            );
            return null;
        });
    }

    /**
     * 获取会话。
     * <p>Redis：HGETALL → 反序列化</p>
     */
    public Session get(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        java.util.Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        Session session = new Session();
        session.setSessionId(str(entries.get("sessionId")));
        session.setSubjectId(str(entries.get("subjectId")));
        String authTypeStr = str(entries.get("authType"));
        if (authTypeStr != null) {
            try {
                session.setAuthType(AuthType.valueOf(authTypeStr));
            } catch (IllegalArgumentException e) {
                // 忽略未知枚举值
            }
        }
        session.setToken(str(entries.get("token")));
        session.setUserIdentity(str(entries.get("userIdentity")));
        session.setCreateTime(longVal(entries.get("createTime")));
        session.setExpireTime(longVal(entries.get("expireTime")));
        return session;
    }

    /**
     * 删除单个会话。
     * <p>Redis：DEL + ZREM</p>
     */
    public void delete(String sessionId) {
        Session session = get(sessionId);
        if (session != null) {
            String key = KEY_PREFIX + sessionId;
            String indexKey = INDEX_PREFIX + session.getSubjectId();
            redisTemplate.delete(key);
            redisTemplate.opsForZSet().remove(indexKey, sessionId);
        }
    }

    /**
     * 按用户删除所有会话。
     * <p>Redis：ZRANGE → Pipeline 批量 DEL → DEL index</p>
     */
    public void deleteBySubjectId(String subjectId) {
        String indexKey = INDEX_PREFIX + subjectId;
        java.util.Set<String> sessionIds = redisTemplate.opsForZSet().range(indexKey, 0, -1);
        if (sessionIds != null && !sessionIds.isEmpty()) {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (String sid : sessionIds) {
                    connection.keyCommands().del((KEY_PREFIX + sid).getBytes());
                }
                return null;
            });
        }
        redisTemplate.delete(indexKey);
    }

    /**
     * 刷新会话 TTL。
     * <p>Redis：EXPIRE</p>
     */
    public void refresh(String sessionId, long ttlSeconds) {
        String key = KEY_PREFIX + sessionId;
        redisTemplate.expire(key, java.time.Duration.ofSeconds(ttlSeconds));
    }

    /**
     * 获取用户所有活跃会话 ID。
     * <p>Redis：ZRANGE</p>
     */
    public List<String> getSessionIds(String subjectId) {
        String indexKey = INDEX_PREFIX + subjectId;
        java.util.Set<String> ids = redisTemplate.opsForZSet().range(indexKey, 0, -1);
        return ids != null ? List.copyOf(ids) : List.of();
    }

    // ========== 工具方法 ==========

    private static String str(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    private static long longVal(Object obj) {
        if (obj == null) return 0L;
        try {
            return Long.parseLong(obj.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

}
