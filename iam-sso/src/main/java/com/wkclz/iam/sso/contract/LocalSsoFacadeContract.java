package com.wkclz.iam.sso.contract;

import com.wkclz.iam.common.entity.IamLoginLog;
import com.wkclz.auth.bean.Principal;
import com.wkclz.iam.contract.bean.RequestLog;
import com.wkclz.iam.contract.bean.Session;
import com.wkclz.iam.contract.bean.req.SessionCreateReq;
import com.wkclz.iam.contract.bean.resp.LoginResp;
import com.wkclz.iam.contract.config.ContractSettings;
import com.wkclz.iam.contract.facade.SsoFacadeContract;
import com.wkclz.iam.sdk.bean.UserJwt;
import com.wkclz.iam.sdk.bean.enums.LoginStatus;
import com.wkclz.iam.sdk.util.JwtUtil;
import com.wkclz.iam.sso.mapper.SsoLoginLogMapper;
import com.wkclz.iam.sso.service.IamRequestService;
import com.wkclz.iam.sso.service.IamSessionService;
import com.wkclz.web.helper.IpHelper;
import com.wkclz.web.helper.RequestHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LocalSsoFacadeContract implements SsoFacadeContract {

    @Autowired
    private IamSessionService iamSessionService;
    @Autowired
    private IamRequestService requestLogService;
    @Autowired
    private SsoLoginLogMapper ssoLoginLogMapper;

    @Override
    public LoginResp login(SessionCreateReq req) {
        log.info("SsoFacade 本地创建会话, authIdentifier: {}", req.getAuthIdentifier());

        UserJwt userJwt = new UserJwt();
        userJwt.setUserCode(req.getUserCode());
        userJwt.setUsername(req.getUsername());
        userJwt.setNickname(req.getNickname());
        userJwt.setAvatar(req.getAvatar());

        Principal principal = new Principal();
        principal.setUserCode(req.getUserCode());
        principal.setUsername(req.getUsername());
        principal.setNickname(req.getNickname());
        principal.setAvatar(req.getAvatar());
        principal.setAuthIdentifier(req.getAuthIdentifier());

        Session session = new Session();
        session.setUserCode(req.getUserCode());
        session.setAuthType(req.getAuthType());
        session.setAuthIdentifier(req.getAuthIdentifier());

        String token = JwtUtil.generateToken(userJwt, ContractSettings.getJwtSecretKey());

        iamSessionService.createSession(token, principal, session);
        iamSessionService.enforceMaxConcurrentSessions(req.getUsername());

        recordLoginLog(req);

        return LoginResp.success(token, req.getUserCode(), req.getUsername(),
                req.getNickname(), req.getAvatar());
    }

    @Override
    public void saveLog(RequestLog log) {
        // 直接委托，无需转换（iam-sdk RequestLog 已删除，统一用契约层）
        requestLogService.insertLog(log);
    }

    @Override
    public void logout(String token) {
        iamSessionService.logout(token);
    }

    private void recordLoginLog(SessionCreateReq req) {
        IamLoginLog loginLog = new IamLoginLog();
        loginLog.setAuthIdentifier(req.getAuthIdentifier());
        loginLog.setAuthType(req.getAuthType());
        loginLog.setLoginStatus(LoginStatus.SUCCESS.getCode());
        loginLog.setMessage(LoginStatus.SUCCESS.getMessage());
        loginLog.setUserCode(req.getUserCode());
        loginLog.setUsername(req.getAuthIdentifier());
        loginLog.setCreateBy(req.getAuthIdentifier());
        loginLog.setUpdateBy(req.getAuthIdentifier());

        // 从请求上下文获取 IP/UA（SessionCreateReq 已删除这两个字段）
        HttpServletRequest request = RequestHelper.getRequest();
        if (request != null) {
            loginLog.setIpAddress(IpHelper.getOriginIp(request));
            loginLog.setUserAgent(request.getHeader("User-Agent"));
        }
        ssoLoginLogMapper.insertLoginLog(loginLog);
    }
}
