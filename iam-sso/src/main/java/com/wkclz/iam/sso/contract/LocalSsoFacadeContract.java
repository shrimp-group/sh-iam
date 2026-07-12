package com.wkclz.iam.sso.contract;

import com.wkclz.auth.bean.Session;
import com.wkclz.auth.contract.auth.ConcurrentSessionControl;
import com.wkclz.auth.contract.auth.SessionStore;
import com.wkclz.iam.common.entity.IamLoginLog;
import com.wkclz.auth.bean.Principal;
import com.wkclz.auth.bean.RequestRecord;
import com.wkclz.iam.sdk.contract.bean.req.SessionCreateReq;
import com.wkclz.iam.sdk.contract.bean.resp.LoginResp;
import com.wkclz.iam.sdk.contract.config.ContractSettings;
import com.wkclz.iam.sdk.contract.facade.SsoFacadeContract;
import com.wkclz.iam.sdk.bean.UserJwt;
import com.wkclz.iam.sdk.bean.enums.LoginStatus;
import com.wkclz.iam.sdk.util.JwtUtil;
import com.wkclz.iam.sso.mapper.SsoLoginLogMapper;
import com.wkclz.iam.sso.service.IamRequestService;
import com.wkclz.web.helper.IpHelper;
import com.wkclz.web.helper.RequestHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class LocalSsoFacadeContract implements SsoFacadeContract {

    @Autowired
    private SessionStore sessionStore;
    @Autowired
    private IamRequestService iamRequestService;
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
        String token = JwtUtil.generateToken(userJwt, ContractSettings.getJwtSecretKey());
        session.setSessionId(token);
        session.setSubjectId(req.getUsername());
        session.setPrincipal(principal);
        session.setAuthType(req.getAuthType());
        session.setAuthIdentifier(req.getAuthIdentifier());
        session.setCreateTime(LocalDateTime.now());

        sessionStore.save(session);
        concurrentSessionControl.enforce(req.getUsername());

        recordLoginLog(req);

        return LoginResp.success(token, req.getUserCode(), req.getUsername(),
                req.getNickname(), req.getAvatar());
    }

    @Override
    public void saveLog(RequestRecord log) {
        // 直接委托，无需转换（已统一用 sh-auth RequestRecord）
        iamRequestService.save(log);
    }

    @Override
    public void logout(String token) {
        sessionStore.delete(token);
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
