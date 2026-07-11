package com.wkclz.auth.contract.auth;

import com.wkclz.auth.exception.AccountStatusException;

public interface AccountStatusChecker {
    void checkStatus(String subjectId) throws AccountStatusException;
}
