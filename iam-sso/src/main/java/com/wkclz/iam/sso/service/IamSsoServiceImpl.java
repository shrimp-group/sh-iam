package com.wkclz.iam.sso.service;

import com.alibaba.fastjson2.JSON;
import com.wkclz.core.identity.UserIdentity;
import com.wkclz.iam.sdk.bean.UserSession;
import com.wkclz.iam.sdk.service.IamSsoService;
import com.wkclz.iam.session.bean.Session;
import com.wkclz.iam.session.service.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IamSsoServiceImpl implements IamSsoService {

    @Autowired
    private SessionManager sessionManager;

    @Override
    public UserSession tokenCheck(String token, String username) {
        Session session = sessionManager.validateAndRefresh(token);
        if (session == null) {
            log.debug("Session 已过期或无效, user={}", username);
            return null;
        }
        UserIdentity userIdentity = JSON.parseObject(session.getUserIdentity(), UserIdentity.class);
        if (userIdentity == null) {
            log.warn("Session 中 userIdentity 解析为空, sessionId={}", session.getSessionId());
            return null;
        }
        UserSession userSession = new UserSession();
        userSession.setUserCode(userIdentity.getUserCode());
        userSession.setUsername(userIdentity.getUsername());
        userSession.setNickname(userIdentity.getNickname());
        userSession.setAuthType(session.getAuthType().name());
        return userSession;
    }
}
