package com.wkclz.auth.contract.auth;

public interface LogoutService {
    void logout();
    void logout(String sessionId);
    void invalidateAllSessions(String subjectId);
}
