package com.wkclz.iam.session.service;

import com.alibaba.fastjson2.JSON;
import com.wkclz.core.identity.UserIdentity;
import com.wkclz.iam.session.bean.Session;
import com.wkclz.iam.session.bean.SessionCreateResult;
import com.wkclz.iam.session.config.IamSessionConfig;
import com.wkclz.iam.session.enums.AuthType;
import com.wkclz.tool.tools.Md5Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 会话管理器 — 会话创建与并发会话控制。
 *
 * <p>认证方式无关的会话创建入口。职责：
 * <ul>
 *   <li>调用 TokenService 生成 JWT</li>
 *   <li>构建 Session 对象（sessionId = MD5(token)）</li>
 *   <li>并发会话控制（可选，通过 iam.session.max-concurrent 配置）</li>
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
        long ttl = iamSessionConfig.getTtl() != null ? iamSessionConfig.getTtl() : 86400L;
        Session session = new Session();
        session.setSessionId(sessionId);
        session.setSubjectId(userIdentity.getUserCode());
        session.setAuthType(authType);
        session.setToken(token);
        session.setCreateTime(now);
        session.setExpireTime(now + ttl * 1000);

        // 序列化 UserIdentity 为 JSON（含 nickname/avatar/attributes 等扩展字段）
        session.setUserIdentity(JSON.toJSONString(userIdentity));

        // 4. 并发控制
        Integer maxConcurrent = iamSessionConfig.getMaxConcurrent();
        if (maxConcurrent != null && maxConcurrent > 0) {
            enforceMaxConcurrent(userIdentity.getUserCode(), maxConcurrent);
        }

        // 5. 持久化
        sessionStore.save(session, ttl);

        log.info("Session created: subjectId={}, sessionId={}, authType={}", userIdentity.getUserCode(), sessionId, authType);

        // 6. 返回结果
        SessionCreateResult result = new SessionCreateResult();
        result.setToken(token);
        result.setSession(session);
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
