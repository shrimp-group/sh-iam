package com.wkclz.iam.session.event;

import com.wkclz.iam.session.cache.AuthCacheManager;
import com.wkclz.iam.session.service.SessionStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 会话终结权限缓存清理钩子 — 监听 {@link SessionEvent}，用户最后一个会话终结时清理其权限缓存。
 *
 * <p>规则：DESTROYED / EXPIRED → 剩余会话为 0 时调
 * {@link AuthCacheManager#evictUser(String, String)}（reason=SESSION_END）；
 * 仍有剩余会话（多端登录）不清理；CREATED 不处理。</p>
 *
 * <p>全方法 try-catch：清理失败仅记日志，不影响会话主流程。</p>
 *
 * @author shrimp
 */
@Slf4j
@Component
public class AuthCacheSessionCleaner {

    @Resource
    private SessionStore sessionStore;
    @Resource
    private AuthCacheManager authCacheManager;

    /**
     * 会话事件处理：终结类事件且无剩余会话时清理用户权限缓存。
     *
     * @param event 会话生命周期事件
     */
    @EventListener
    public void onSessionEvent(SessionEvent event) {
        try {
            if (event.getType() != SessionEvent.Type.DESTROYED && event.getType() != SessionEvent.Type.EXPIRED) {
                return;
            }
            String subjectId = event.getSubjectId();
            List<String> remaining = sessionStore.getSessionIds(subjectId);
            if (remaining == null || remaining.isEmpty()) {
                authCacheManager.evictUser(subjectId, "SESSION_END");
                log.info("[auth-cache] 会话终结清理用户权限缓存: subjectId={}, type={}, reason={}", subjectId, event.getType(), event.getReason());
            } else {
                log.debug("[auth-cache] 用户仍有剩余会话，权限缓存保留: subjectId={}, remaining={}", subjectId, remaining.size());
            }
        } catch (Exception e) {
            log.error("会话终结权限缓存清理异常（不影响主流程）: event={}", event, e);
        }
    }
}
