package com.wkclz.auth.contract.infra;

import com.wkclz.auth.bean.RequestRecord;

public interface RequestLogger {
    void save(RequestRecord record);
}
