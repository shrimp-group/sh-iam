package com.wkclz.iam.session.service;

import com.wkclz.core.identity.UserIdentity;
import com.wkclz.iam.session.bean.Session;
import com.wkclz.iam.session.bean.SessionCreateResult;
import com.wkclz.iam.session.config.IamSessionConfig;
import com.wkclz.iam.session.enums.AuthType;
import com.wkclz.iam.session.enums.DestroyReason;
import com.wkclz.iam.session.event.SessionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionManager 会话管理器")
class SessionManagerTest {

    private static final String SECRET_KEY = "this-is-a-test-secret-key-which-is-32-chars!";
    private static final long TOKEN_TTL = 86400L;
    private static final long REDIS_TTL = 3600L;  // 1h for test convenience
    private static final long RENEWAL_THRESHOLD = 3600L; // 1h, = REDIS_TTL to disable fast-path by default
    private static final long RENEWAL_INTERVAL = 300L;   // 5min

    @Mock
    private SessionStore sessionStore;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private IamSessionConfig config;
    private TokenService tokenService;
    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        // 准备配置
        config = new IamSessionConfig();
        config.setSecretKey(SECRET_KEY);
        config.setTtl(TOKEN_TTL);
        config.setRedisTtl(REDIS_TTL);
        config.setRenewalThreshold(RENEWAL_THRESHOLD);
        config.setRenewalInterval(RENEWAL_INTERVAL);
        config.setMaxConcurrent(0);

        // 准备 TokenService（field injection 模拟）
        tokenService = new TokenService();
        ReflectionTestUtils.setField(tokenService, "iamSessionConfig", config);

        // 准备 SessionManager（field injection 模拟）
        sessionManager = new SessionManager();
        ReflectionTestUtils.setField(sessionManager, "tokenService", tokenService);
        ReflectionTestUtils.setField(sessionManager, "sessionStore", sessionStore);
        ReflectionTestUtils.setField(sessionManager, "iamSessionConfig", config);
        ReflectionTestUtils.setField(sessionManager, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(sessionManager, "eventPublisher", eventPublisher);
    }

    private UserIdentity createUser(String userCode, String username, String nickname) {
        UserIdentity user = new UserIdentity();
        user.setUserCode(userCode);
        user.setUsername(username);
        user.setNickname(nickname);
        return user;
    }

    private Session buildSession(String sessionId, String subjectId, long redisExpireTime, long lastRenewalTime) {
        Session session = new Session();
        session.setSessionId(sessionId);
        session.setSubjectId(subjectId);
        session.setAuthType(AuthType.PASSWORD);
        session.setCreateTime(System.currentTimeMillis() - 3600_000);
        session.setExpireTime(System.currentTimeMillis() + TOKEN_TTL * 1000);
        session.setRedisExpireTime(redisExpireTime);
        session.setLastRenewalTime(lastRenewalTime);
        return session;
    }

    @Nested
    @DisplayName("会话创建")
    class CreateSession {

        @Test
        @DisplayName("传入 UserIdentity + authType → 返回 SessionCreateResult(token, session) + 发布创建事件")
        void shouldCreateSession() {
            UserIdentity user = createUser("user_001", "admin", "管理员");

            SessionCreateResult result = sessionManager.createSession(user, AuthType.PASSWORD);

            assertNotNull(result);
            assertNotNull(result.getToken(), "token 不应为空");
            assertNotNull(result.getSession(), "session 不应为空");
            assertEquals("user_001", result.getSession().getSubjectId());
            assertEquals(AuthType.PASSWORD, result.getSession().getAuthType());
            // 验证发布 SessionCreatedEvent
            verify(eventPublisher, times(1)).publishEvent(any(SessionEvent.class));
        }

        @Test
        @DisplayName("Token 与 Session 关联正确：sessionId = MD5(token)")
        void shouldLinkTokenAndSessionViaMd5() {
            UserIdentity user = createUser("user_002", "user2", "用户2");

            SessionCreateResult result = sessionManager.createSession(user, AuthType.WECHAT_MINI);

            assertNotNull(result.getSession().getSessionId());
            assertEquals(32, result.getSession().getSessionId().length(),
                "sessionId 应为 MD5 的 32 字符长度");
            assertEquals(result.getToken(), result.getSession().getToken());
            assertNotNull(result.getSession().getUserIdentity());
            assertTrue(result.getSession().getUserIdentity().contains("user_002"));
        }

        @Test
        @DisplayName("会话创建成功后 Session 已持久化到 Redis")
        void shouldPersistSession() {
            UserIdentity user = createUser("user_003", "user3", "用户3");

            sessionManager.createSession(user, AuthType.PASSWORD);

            verify(sessionStore, times(1)).save(any(Session.class), eq(REDIS_TTL));
        }
    }

    @Nested
    @DisplayName("会话验证与续期")
    class ValidateAndRefresh {

        @Test
        @DisplayName("Token 签发时间离 Redis 过期还很远时走快速路径，不读 Redis")
        void shouldTakeFastPathWhenTokenIsFresh() {
            // 设置 threshold < redisTtl，触发快速路径判断
            config.setRenewalThreshold(1800L);
            UserIdentity user = createUser("user_fast", "fastpath", "快速路径");
            String token = tokenService.generateToken(user.getUserCode(), user.getUsername(), user.getNickname());

            Session result = sessionManager.validateAndRefresh(token);

            assertNotNull(result);
            assertEquals("user_fast", result.getSubjectId());
            assertNotNull(result.getSessionId());
            assertEquals(token, result.getToken());
            // 快速路径下不应读取 Redis
            verify(sessionStore, never()).get(anyString());
        }

        @Test
        @DisplayName("Session 不存在时返回 null")
        void shouldReturnNullWhenSessionNotFound() {
            UserIdentity user = createUser("user_001", "admin", "管理员");
            String token = tokenService.generateToken(user.getUserCode(), user.getUsername(), user.getNickname());

            when(sessionStore.get(anyString())).thenReturn(null);

            Session result = sessionManager.validateAndRefresh(token);

            assertNull(result);
        }

        @Test
        @DisplayName("Redis 已过期时返回 null + 发布过期事件 + 删除 Session")
        void shouldReturnNullWhenRedisExpired() {
            UserIdentity user = createUser("user_002", "user2", "用户2");
            String token = tokenService.generateToken(user.getUserCode(), user.getUsername(), user.getNickname());

            long now = System.currentTimeMillis();
            Session expired = buildSession("sid_002", "user_002", now - 1000, now - 3600_000);
            when(sessionStore.get(anyString())).thenReturn(expired);

            Session result = sessionManager.validateAndRefresh(token);

            assertNull(result);
            // 验证发布 SessionExpiredEvent 并删除过期 Session
            verify(eventPublisher, times(1)).publishEvent(any(SessionEvent.class));
            verify(sessionStore, times(1)).delete("sid_002");
        }

        @Test
        @DisplayName("剩余 TTL < 阈值时触发续期")
        void shouldRenewWhenBelowThreshold() {
            UserIdentity user = createUser("user_003", "user3", "用户3");
            String token = tokenService.generateToken(user.getUserCode(), user.getUsername(), user.getNickname());

            long now = System.currentTimeMillis();
            // Redis 剩余 10 分钟（< 1h 阈值）
            long redisExpireTime = now + 600_000;
            // 上次续期 1 小时前（> 5min 间隔）
            long lastRenewalTime = now - 3600_000;
            Session session = buildSession("sid_003", "user_003", redisExpireTime, lastRenewalTime);
            when(sessionStore.get(anyString())).thenReturn(session);

            Session result = sessionManager.validateAndRefresh(token);

            assertNotNull(result);
            // 验证续期被调用
            verify(sessionStore, times(1)).renewSession(eq("sid_003"), anyLong());
            // 验证内存中字段已更新
            assertTrue(result.getRedisExpireTime() > redisExpireTime, "redisExpireTime 应该已更新");
            assertTrue(result.getLastRenewalTime() > lastRenewalTime, "lastRenewalTime 应该已更新");
        }

        @Test
        @DisplayName("距上次续期 < interval 时不重复续期")
        void shouldNotRenewWithinInterval() {
            UserIdentity user = createUser("user_004", "user4", "用户4");
            String token = tokenService.generateToken(user.getUserCode(), user.getUsername(), user.getNickname());

            long now = System.currentTimeMillis();
            // Redis 剩余 10 分钟（< 1h 阈值）
            long redisExpireTime = now + 600_000;
            // 上次续期刚刚（< 5min 间隔）
            long lastRenewalTime = now;
            Session session = buildSession("sid_004", "user_004", redisExpireTime, lastRenewalTime);
            when(sessionStore.get(anyString())).thenReturn(session);

            Session result = sessionManager.validateAndRefresh(token);

            assertNotNull(result);
            // 验证续期未被调用
            verify(sessionStore, never()).renewSession(anyString(), anyLong());
        }

        @Test
        @DisplayName("有效 Session 正常返回")
        void shouldReturnValidSession() {
            UserIdentity user = createUser("user_005", "user5", "用户5");
            String token = tokenService.generateToken(user.getUserCode(), user.getUsername(), user.getNickname());

            long now = System.currentTimeMillis();
            // Redis 剩余 2 小时（> 1h 阈值，不触发续期）
            long redisExpireTime = now + 7200_000;
            long lastRenewalTime = now - 3600_000;
            Session session = buildSession("sid_005", "user_005", redisExpireTime, lastRenewalTime);
            when(sessionStore.get(anyString())).thenReturn(session);

            Session result = sessionManager.validateAndRefresh(token);

            assertNotNull(result);
            assertEquals("user_005", result.getSubjectId());
            // 剩余充足，不触发续期
            verify(sessionStore, never()).renewSession(anyString(), anyLong());
        }
    }

    @Nested
    @DisplayName("活跃会话查询")
    class GetActiveSessions {

        @Test
        @DisplayName("无活跃会话时返回空列表")
        void shouldReturnEmptyWhenNoSessions() {
            String subjectId = "user_empty";
            when(sessionStore.getSessionIds(subjectId)).thenReturn(List.of());

            List<Session> result = sessionManager.getActiveSessions(subjectId);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("只返回未过期的会话")
        void shouldReturnOnlyActiveSessions() {
            String subjectId = "user_multi";
            long now = System.currentTimeMillis();

            // 3 个 session：1 个过期，2 个活跃
            Session active1 = buildSession("sid_a1", subjectId, now + 3600_000, now);
            Session active2 = buildSession("sid_a2", subjectId, now + 7200_000, now);
            Session expired = buildSession("sid_exp", subjectId, now - 1000, now);

            when(sessionStore.getSessionIds(subjectId)).thenReturn(List.of("sid_a1", "sid_a2", "sid_exp"));
            when(sessionStore.get("sid_a1")).thenReturn(active1);
            when(sessionStore.get("sid_a2")).thenReturn(active2);
            when(sessionStore.get("sid_exp")).thenReturn(expired);

            List<Session> result = sessionManager.getActiveSessions(subjectId);

            assertEquals(2, result.size());
            assertTrue(result.contains(active1));
            assertTrue(result.contains(active2));
            assertFalse(result.contains(expired));
        }
    }

    @Nested
    @DisplayName("会话销毁")
    class SessionDestroy {

        @Test
        @DisplayName("销毁存在的会话 → 返回 true，删除 Redis + 发布事件")
        void shouldDestroyExistingSession() {
            Session session = buildSession("sid_d1", "user_del", System.currentTimeMillis() + 3600_000, System.currentTimeMillis());
            when(sessionStore.get("sid_d1")).thenReturn(session);

            boolean result = sessionManager.destroySession("sid_d1", DestroyReason.LOGOUT);

            assertTrue(result);
            verify(sessionStore, times(1)).delete("sid_d1");
            verify(eventPublisher, times(1)).publishEvent(any(SessionEvent.class));
        }

        @Test
        @DisplayName("销毁不存在的会话 → 返回 false，不删 Redis")
        void shouldReturnFalseWhenSessionNotFound() {
            when(sessionStore.get("sid_nonexist")).thenReturn(null);

            boolean result = sessionManager.destroySession("sid_nonexist", DestroyReason.LOGOUT);

            assertFalse(result);
            verify(sessionStore, never()).delete(anyString());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("批量销毁有活跃会话 → 返回数量，删除所有 + 逐个发布事件")
        void shouldDestroyAllActiveSessions() {
            String subjectId = "user_batch";
            List<String> sessionIds = List.of("sid_b1", "sid_b2", "sid_b3");
            when(sessionStore.getSessionIds(subjectId)).thenReturn(sessionIds);

            int result = sessionManager.destroyAllSessions(subjectId, DestroyReason.PASSWORD_CHANGED);

            assertEquals(3, result);
            verify(sessionStore, times(1)).deleteBySubjectId(subjectId);
            verify(eventPublisher, times(3)).publishEvent(any(SessionEvent.class));
        }

        @Test
        @DisplayName("批量销毁无活跃会话 → 返回 0，不删不发布事件")
        void shouldReturnZeroWhenNoActiveSessions() {
            String subjectId = "user_empty";
            when(sessionStore.getSessionIds(subjectId)).thenReturn(List.of());

            int result = sessionManager.destroyAllSessions(subjectId, DestroyReason.USER_DISABLED);

            assertEquals(0, result);
            verify(sessionStore, never()).deleteBySubjectId(anyString());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("并发控制")
    class ConcurrentControl {

        @Test
        @DisplayName("maxConcurrent=0 时不限制")
        void shouldNotLimitWhenZero() {
            UserIdentity user = createUser("user_no_limit", "nolimit", "无限制");

            sessionManager.createSession(user, AuthType.PASSWORD);

            verify(sessionStore, times(1)).save(any(Session.class), anyLong());
        }

        @Test
        @DisplayName("maxConcurrent=1 时第一个会话正常创建")
        void shouldAllowFirstSession() {
            config.setMaxConcurrent(1);

            UserIdentity user = createUser("user_single", "single", "单会话用户");

            SessionCreateResult result = sessionManager.createSession(user, AuthType.PASSWORD);

            assertNotNull(result);
            assertNotNull(result.getToken());
        }
    }

    @Nested
    @DisplayName("配置")
    class Configuration {

        @Test
        @DisplayName("默认 maxConcurrent 为 0（不限制）")
        void defaultMaxConcurrent() {
            assertEquals(0, config.getMaxConcurrent().intValue());
        }
    }

}
