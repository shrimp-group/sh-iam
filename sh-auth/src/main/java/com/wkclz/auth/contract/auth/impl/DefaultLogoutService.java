package com.wkclz.auth.contract.auth.impl;

import com.wkclz.auth.bean.Principal;
import com.wkclz.auth.context.SecurityContext;
import com.wkclz.auth.contract.auth.LogoutService;
import com.wkclz.auth.contract.auth.SessionStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DefaultLogoutService implements LogoutService {
    private final SessionStore sessionStore;

    @Override
    public void logout() {
        String token = SecurityContext.getToken();
        if (token != null) {
            sessionStore.delete(token);
            Principal principal = SecurityContext.getPrincipal();
            log.debug("已登出当前会话: user={}", principal != null ? principal.getUsername() : "unknown");
        }
    }

    @Override
    public void logout(String sessionId) {
        sessionStore.delete(sessionId);
        log.debug("已踢出会话: sessionId={}", sessionId);
    }

    @Override
    public void invalidateAllSessions(String subjectId) {
        sessionStore.deleteBySubjectId(subjectId);
        log.info("已使所有会话失效: subjectId={}", subjectId);
    }
}
