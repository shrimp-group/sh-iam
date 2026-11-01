package com.wkclz.iam.session.event;

import com.wkclz.iam.session.bean.Session;
import com.wkclz.iam.session.cache.AuthCacheManager;
import com.wkclz.iam.session.enums.DestroyReason;
import com.wkclz.iam.session.service.SessionStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthCacheSessionCleaner 单元测试 — 会话终结时按剩余会话数决定是否清理用户权限缓存。
 */
@ExtendWith(MockitoExtension.class)
class AuthCacheSessionCleanerTest {

    @Mock
    private AuthCacheManager authCacheManager;
    @Mock
    private SessionStore sessionStore;
    @InjectMocks
    private AuthCacheSessionCleaner cleaner;

    @Test
    void evictsCacheWhenNoRemainingSessions() {
        when(sessionStore.getSessionIds("user_1")).thenReturn(List.of());
        cleaner.onSessionEvent(SessionEvent.destroyed("sid_1", "user_1", DestroyReason.LOGOUT));
        verify(authCacheManager).evictUser("user_1", "SESSION_END");
    }

    @Test
    void evictsCacheOnExpiredWhenNoRemainingSessions() {
        when(sessionStore.getSessionIds("user_2")).thenReturn(null);
        cleaner.onSessionEvent(SessionEvent.expired("sid_2", "user_2"));
        verify(authCacheManager).evictUser("user_2", "SESSION_END");
    }

    @Test
    void keepsCacheWhenRemainingSessionsExist() {
        when(sessionStore.getSessionIds("user_3")).thenReturn(List.of("sid_other"));
        cleaner.onSessionEvent(SessionEvent.destroyed("sid_3", "user_3", DestroyReason.LOGOUT));
        verify(authCacheManager, never()).evictUser(anyString(), anyString());
    }

    @Test
    void ignoresCreatedEvent() {
        Session session = new Session();
        session.setSessionId("sid_new");
        session.setSubjectId("user_new");
        cleaner.onSessionEvent(SessionEvent.created(session));
        verify(authCacheManager, never()).evictUser(anyString(), anyString());
        Mockito.verifyNoInteractions(sessionStore);
    }

    @Test
    void cleanupErrorDoesNotAffectMainFlow() {
        when(sessionStore.getSessionIds("user_err")).thenReturn(List.of());
        Mockito.doThrow(new RuntimeException("redis down"))
            .when(authCacheManager).evictUser(anyString(), anyString());
        assertDoesNotThrow(() ->
            cleaner.onSessionEvent(SessionEvent.destroyed("sid_err", "user_err", DestroyReason.LOGOUT)));
    }
}
