package com.wkclz.iam.sdk.facade;


import com.wkclz.iam.sdk.model.RequestLog;

public interface SsoFacade {

    void saveLog(RequestLog log);

}
