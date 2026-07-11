package com.wkclz.auth.contract.auth;

public interface ConcurrentSessionControl {
    void enforce(String subjectId);
    int getCurrentCount(String subjectId);
    int getMaxSessions();
}
