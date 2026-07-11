package com.wkclz.auth.contract.auth;

import com.wkclz.auth.bean.Session;
import java.util.List;

public interface SessionStore {
    void save(Session session);
    Session get(String sessionId);
    void delete(String sessionId);
    void deleteBySubjectId(String subjectId);
    void refresh(String sessionId, long ttlSeconds);
    List<Session> getActiveSessions(String subjectId);
}
