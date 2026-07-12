package com.wkclz.iam.sso.contract;

import com.wkclz.auth.contract.auth.ConcurrentSessionControl;
import com.wkclz.iam.common.entity.IamLoginLog;
import com.wkclz.auth.bean.Principal;
import com.wkclz.auth.bean.RequestRecord;
import com.wkclz.iam.sdk.contract.bean.Session;
import com.wkclz.iam.sdk.contract.bean.req.SessionCreateReq;
import com.wkclz.iam.sdk.contract.bean.resp.LoginResp;
import com.wkclz.iam.sdk.contract.config.ContractSettings;
import com.wkclz.iam.sdk.contract.facade.SsoFacadeContract;
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
    @Autowired
    private ConcurrentSessionControl concurrentSessionControl;

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
        concurrentSessionControl.enforce(req.getUsername());

        recordLoginLog(req);

        return LoginResp.success(token, req.getUserCode(), req.getUsername(),
                req.getNickname(), req.getAvatar());
    }

    @Override
    public void saveLog(RequestRecord log) {
        // 直接委托，无需转换（已统一用 sh-auth RequestRecord）
        requestLogService.save(log);
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
