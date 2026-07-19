package com.wkclz.iam.session.service;

import com.alibaba.fastjson2.JSON;
import com.wkclz.core.identity.UserIdentity;
import com.wkclz.iam.session.bean.Session;
import com.wkclz.iam.session.bean.SessionCreateResult;
import com.wkclz.iam.session.bean.TokenInfo;
import com.wkclz.iam.session.config.IamSessionConfig;
import com.wkclz.iam.session.enums.AuthType;
import com.wkclz.iam.session.enums.DestroyReason;
import com.wkclz.iam.session.event.SessionEvent;
import com.wkclz.tool.tools.Md5Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 会话管理器 — 会话创建、验证、续期、销毁与并发会话控制。
 *
 * <p>认证方式无关的会话管理入口。职责：
 * <ul>
 *   <li>调用 TokenService 生成/验证 JWT</li>
 *   <li>构建 Session 对象（sessionId = MD5(token)）</li>
 *   <li>并发会话控制（可选，通过 iam.session.max-concurrent 配置）</li>
 *   <li>会话验证与滑窗续期（validateAndRefresh）</li>
 *   <li>活跃会话查询（getActiveSessions）</li>
 *   <li>会话销毁（destroySession / destroyAllSessions）</li>
 *   <li>通过 SessionStore 持久化到 Redis</li>
 * </ul>
 */
@Component
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    @Autowired
    private TokenService tokenService;
    @Autowired
    private SessionStore sessionStore;
    @Autowired
    private IamSessionConfig iamSessionConfig;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private final DefaultRedisScript<Long> evictScript = buildEvictScript();


    /**
     * 创建会话。
     *
     * <ol>
     *   <li>TokenService.generateToken() 生成 JWT</li>
     *   <li>sessionId = MD5(token)</li>
     *   <li>并发控制：若活跃会话数 ≥ maxConcurrent，踢出最早会话</li>
     *   <li>SessionStore.save() 持久化</li>
     *   <li>返回 SessionCreateResult</li>
     * </ol>
     *
     * @param userIdentity 用户身份（必填 userCode/username/nickname）
     * @param authType     认证方式
     * @return 会话创建结果（含 token 和 session）
     */
    public SessionCreateResult createSession(UserIdentity userIdentity, AuthType authType) {
        // 1. 生成 Token
        String token = tokenService.generateToken(userIdentity.getUserCode(), userIdentity.getUsername(), userIdentity.getNickname());

        // 2. sessionId = MD5(token)
        String sessionId = Md5Tool.md5(token);

        // 3. 构建 Session
        long now = System.currentTimeMillis();
        long ttl = iamSessionConfig.getTtl() != null ? iamSessionConfig.getTtl() : 172800L;
        long redisTtl = iamSessionConfig.getRedisTtl() != null ? iamSessionConfig.getRedisTtl() : 86400L;
        Session session = new Session();
        session.setSessionId(sessionId);
        session.setSubjectId(userIdentity.getUserCode());
        session.setAuthType(authType);
        session.setToken(token);
        session.setCreateTime(now);
        session.setExpireTime(now + ttl * 1000);
        session.setRedisExpireTime(now + redisTtl * 1000);
        session.setLastRenewalTime(now);

        // 序列化 UserIdentity 为 JSON（含 nickname/avatar/attributes 等扩展字段）
        session.setUserIdentity(JSON.toJSONString(userIdentity));

        // 4. 并发控制
        Integer maxConcurrent = iamSessionConfig.getMaxConcurrent();
        if (maxConcurrent != null && maxConcurrent > 0) {
            enforceMaxConcurrent(userIdentity.getUserCode(), maxConcurrent);
        }

        // 5. 持久化（使用 redisTtl 控制 Redis Key 过期时间）
        sessionStore.save(session, redisTtl);

        log.info("Session created: subjectId={}, sessionId={}, authType={}", userIdentity.getUserCode(), sessionId, authType);

        // 6. 发布 SessionCreatedEvent
        eventPublisher.publishEvent(SessionEvent.created(session));

        // 7. 返回结果
        SessionCreateResult result = new SessionCreateResult();
        result.setToken(token);
        result.setSession(session);
        return result;
    }

    // ========== 会话验证与续期 ==========

    /**
     * 验证 Token 并执行滑窗续期。
     *
     * <ol>
     *   <li>TokenService.verifyToken() 校验 JWT 签名/过期</li>
     *   <li>MD5(token) 得到 sessionId</li>
     *   <li>SessionStore.get(sessionId) 查 Redis</li>
     *   <li>Session 不存在或 Redis 已过期 → 返回 null</li>
     *   <li>续期判断：剩余 TTL < threshold && 距上次续期 > interval && JWT 还有空间</li>
     *   <li>续期只操作 Redis（HSET + EXPIRE），不重新签发 JWT</li>
     * </ol>
     *
     * @param token JWT Token
     * @return 有效的 Session，无效时返回 null
     */
    public Session validateAndRefresh(String token) {
        // 1. JWT 校验（签名/过期，由 JJWT 库处理）
        TokenInfo tokenInfo = tokenService.verifyToken(token);
        long now = System.currentTimeMillis();

        // 2. sessionId = MD5(token)
        String sessionId = Md5Tool.md5(token);

        // 3. 查 Redis
        Session session = sessionStore.get(sessionId);
        if (session == null) {
            log.warn("Session not found in Redis: sessionId={}", sessionId);
            return null;
        }

        // 4. Redis 已过期 → 发布 SessionExpiredEvent，清理 Session，返回 null
        if (session.getRedisExpireTime() < now) {
            log.warn("Session Redis expired: sessionId={}, redisExpireTime={}", sessionId, session.getRedisExpireTime());
            eventPublisher.publishEvent(SessionEvent.expired(sessionId, session.getSubjectId()));
            sessionStore.delete(sessionId);
            return null;
        }

        // 5. 快速路径：若 Token 签发时间到 Redis 过期还有大量剩余，跳过 Redis 读取
        long redisTtlSec = iamSessionConfig.getRedisTtl() != null ? iamSessionConfig.getRedisTtl() : 86400L;
        long thresholdSec = iamSessionConfig.getRenewalThreshold() != null ? iamSessionConfig.getRenewalThreshold() : 1800L;
        Long issuedAt = tokenInfo.getIssuedAt();
        if (issuedAt != null && now < issuedAt + (redisTtlSec - thresholdSec) * 1000) {
            log.debug("Session fast-path: Redis expiry is far away, skip read. sessionId={}", sessionId);
            return session;
        }

        // 6. 续期判断
        long thresholdMs = thresholdSec * 1000;
        long intervalMs = (iamSessionConfig.getRenewalInterval() != null ? iamSessionConfig.getRenewalInterval() : 300L) * 1000;
        long remaining = session.getRedisExpireTime() - now;
        long lastRenewalAgo = now - session.getLastRenewalTime();

        if (remaining < thresholdMs && lastRenewalAgo > intervalMs) {
            long jwtExpireAt = tokenInfo.getExpireAt() != null ? tokenInfo.getExpireAt() : Long.MAX_VALUE;
            if (jwtExpireAt > now + thresholdMs) {
                long newRedisExpire = Math.min(now + thresholdMs, jwtExpireAt);
                sessionStore.renewSession(sessionId, newRedisExpire);
                session.setRedisExpireTime(newRedisExpire);
                session.setLastRenewalTime(now);
                log.info("Session renewed: sessionId={}, newRedisExpireTime={}", sessionId, newRedisExpire);
            } else {
                log.debug("Session renewal skipped (JWT near expiry): sessionId={}, jwtExpireAt={}", sessionId, jwtExpireAt);
            }
        }

        return session;
    }

    // ========== 会话销毁 ==========

    /**
     * 销毁单个会话。
     *
     * <p>删除 Redis Session Key + 从用户索引 ZSet 移除，并发布 SessionDestroyedEvent。</p>
     *
     * @param sessionId 会话 ID
     * @param reason    销毁原因
     * @return true 表示成功销毁，false 表示会话不存在
     */
    public boolean destroySession(String sessionId, DestroyReason reason) {
        Session session = sessionStore.get(sessionId);
        if (session == null) {
            log.warn("Session not found for destroy: sessionId={}", sessionId);
            return false;
        }
        String subjectId = session.getSubjectId();
        sessionStore.delete(sessionId);
        eventPublisher.publishEvent(SessionEvent.destroyed(sessionId, subjectId, reason));
        log.info("Session destroyed: sessionId={}, subjectId={}, reason={}", sessionId, subjectId, reason);
        return true;
    }

    /**
     * 批量销毁用户所有会话（如改密、禁用等场景）。
     *
     * <p>通过 ZSet 获取全量 sessionId → Pipeline 批量 DEL + DEL index Key，
     * 并对每个 sessionId 发布 SessionDestroyedEvent。</p>
     *
     * @param subjectId 用户标识（userCode）
     * @param reason    销毁原因
     * @return 实际销毁的会话数量
     */
    public int destroyAllSessions(String subjectId, DestroyReason reason) {
        List<String> sessionIds = sessionStore.getSessionIds(subjectId);
        if (sessionIds.isEmpty()) {
            log.debug("No active sessions to destroy for subjectId={}", subjectId);
            return 0;
        }
        int count = sessionIds.size();
        sessionStore.deleteBySubjectId(subjectId);
        for (String sid : sessionIds) {
            eventPublisher.publishEvent(SessionEvent.destroyed(sid, subjectId, reason));
        }
        log.info("All sessions destroyed: subjectId={}, count={}, reason={}", subjectId, count, reason);
        return count;
    }

    // ========== 活跃会话查询 ==========

    /**
     * 查询用户所有活跃会话。
     *
     * <p>通过 SessionStore 获取所有 sessionId，批量读取后按 redisExpireTime 过滤已过期的。</p>
     *
     * @param subjectId 用户标识（userCode）
     * @return 活跃 Session 列表，无活跃会话时返回空列表
     */
    public List<Session> getActiveSessions(String subjectId) {
        List<String> sessionIds = sessionStore.getSessionIds(subjectId);
        if (sessionIds.isEmpty()) {
            return List.of();
        }
        long now = System.currentTimeMillis();
        List<Session> result = new ArrayList<>();
        for (String sid : sessionIds) {
            Session session = sessionStore.get(sid);
            if (session != null && session.getRedisExpireTime() >= now) {
                result.add(session);
            }
        }
        return result;
    }

    // ========== 并发控制 ==========

    /**
     * 并发会话控制：通过 Lua 脚本原子性执行 ZCARD + ZRANGE + DEL + ZREM。
     *
     * <p>当活跃会话数 ≥ maxConcurrent 时，踢出创建时间最早的会话。</p>
     */
    private void enforceMaxConcurrent(String subjectId, int maxConcurrent) {
        String indexKey = SessionStore.INDEX_PREFIX + subjectId;

        Long evicted = redisTemplate.execute(
            evictScript,
            Collections.singletonList(indexKey),
            String.valueOf(maxConcurrent)
        );

        if (evicted != null && evicted > 0) {
            log.warn("Concurrent session limit reached ({}), evicted {} session(s) for subjectId={}",
                maxConcurrent, evicted, subjectId);
        }
    }

    // ========== Lua 脚本 ==========

    /**
     * 构建并发踢出 Lua 脚本。
     *
     * <p>KEYS[1] = index key (iam:session:index:{subjectId})<br>
     * ARGV[1] = maxConcurrent</p>
     *
     * <p>逻辑：循环 ZCARD 检查是否超限 → ZRANGE 取最早 → DEL + ZREM 直到不超限</p>
     */
    private static DefaultRedisScript<Long> buildEvictScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(
            "local indexKey = KEYS[1]\n" +
                "local maxConcurrent = tonumber(ARGV[1])\n" +
                "local keyPrefix = 'iam:session:'\n" +
                "local evicted = 0\n" +
                "\n" +
                "while true do\n" +
                "    local count = redis.call('ZCARD', indexKey)\n" +
                "    if count < maxConcurrent then\n" +
                "        break\n" +
                "    end\n" +
                "    local members = redis.call('ZRANGE', indexKey, 0, 0)\n" +
                "    if #members == 0 then\n" +
                "        break\n" +
                "    end\n" +
                "    local sessionId = members[1]\n" +
                "    redis.call('DEL', keyPrefix .. sessionId)\n" +
                "    redis.call('ZREM', indexKey, sessionId)\n" +
                "    evicted = evicted + 1\n" +
                "end\n" +
                "return evicted"
        );
        script.setResultType(Long.class);
        return script;
    }

}
