package com.wkclz.auth.contract.auth;

public interface PasswordValidator {
    void validateStrength(String password);
    boolean isHistoryPassword(String subjectId, String password);
    boolean isExpired(String subjectId);
    long getRemainingDays(String subjectId);
}
