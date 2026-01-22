package com.wkclz.iam.sso.service;

import com.wkclz.iam.sdk.facade.SsoFacade;
import com.wkclz.iam.sdk.model.RequestLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SsoFacadeImpl implements SsoFacade {

    @Autowired
    private IamRequestService requestLogService;

    @Override
    public void saveLog(RequestLog log) {
        requestLogService.insertLog(log);
    }

}
