package com.wkclz.iam.sso.service;

import com.wkclz.core.identity.IdentityContext;
import com.wkclz.iam.sdk.facade.SsoFacade;
import com.wkclz.iam.session.bean.RequestRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SsoFacadeImpl implements SsoFacade {

    private static final Logger log = LoggerFactory.getLogger(SsoFacadeImpl.class);

    @Autowired
    private IamRequestService requestLogService;

    @Autowired
    private IamSessionService iamSessionService;

    @Override
    public void saveLog(RequestRecord record) {
        requestLogService.insertLog(log);
    }

    @Override
    public void logout(String token) {
        log.info("SsoFacade 本地登出, token: {}", token);
        iamSessionService.logout(token);
    }

    @Override
    public void logout() {
        String token = IdentityContext.getToken();
        if (StringUtils.isBlank(token)) {
            log.warn("SsoFacade 本地登出，当前请求上下文中 token 为空，跳过登出处理");
            return;
        }
        logout(token);
    }

}
