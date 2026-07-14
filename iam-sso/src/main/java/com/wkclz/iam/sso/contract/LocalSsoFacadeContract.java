package com.wkclz.iam.sso.contract;

import com.wkclz.auth.bean.Principal;
import com.wkclz.auth.bean.RequestRecord;
import com.wkclz.auth.bean.SessionCreateReq;
import com.wkclz.auth.bean.LoginResp;
import com.wkclz.auth.contract.auth.SessionStore;
import com.wkclz.auth.contract.auth.StandardLoginPipeline;
import com.wkclz.iam.common.entity.IamLoginLog;
import com.wkclz.iam.sso.contract.facade.SsoFacadeContract;
import com.wkclz.iam.sso.mapper.SsoLoginLogMapper;
import com.wkclz.iam.sso.service.IamRequestService;
import com.wkclz.web.helper.IpHelper;
import com.wkclz.web.helper.RequestHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * SsoFacade 本地实现 — 委托 sh-auth {@link StandardLoginPipeline} 创建会话。
 * <p>
 * 职责变更：原先手动执行 Token 生成 → Session 构建 → 持久化 → 并发控制，
 * 现已统一委托标准登录管道处理，消除与 {@link com.wkclz.auth.contract.auth.LoginService} 的重复逻辑。
 * </p>
 *
 * @author shrimp
 */
@Slf4j
@Component
public class LocalSsoFacadeContract implements SsoFacadeContract {

    @Autowired
    private StandardLoginPipeline pipeline;
    @Autowired
    private SessionStore sessionStore;
    @Autowired
    private IamRequestService iamRequestService;
    @Autowired
    private SsoLoginLogMapper ssoLoginLogMapper;

    @Override
    public LoginResp login(SessionCreateReq req) {
        log.info("SsoFacade 本地创建会话, authIdentifier: {}", req.getAuthIdentifier());

        // 构建 Principal
        Principal principal = new Principal();
        principal.setUserCode(req.getUserCode());
        principal.setUsername(req.getUsername());
        principal.setNickname(req.getNickname());
        principal.setAvatar(req.getAvatar());
        principal.setAuthIdentifier(req.getAuthIdentifier());

        // 通过标准登录管道创建会话（Token → Session → 持久化 → 并发控制）
        StandardLoginPipeline.PipelineResult pipelineResult = pipeline.execute(
            principal,
            req.getAuthType(),
            null,
            null
        );

        // 记录登录日志
        recordLoginLog(req);

        return LoginResp.success(
            pipelineResult.getTokenValue(),
            req.getUserCode(),
            req.getUsername(),
            req.getNickname(),
            req.getAvatar()
        );
    }

    @Override
    public void saveLog(RequestRecord log) {
        iamRequestService.save(log);
    }

    @Override
    public void logout(String token) {
        sessionStore.delete(token);
        log.info("SsoFacade 登出完成");
    }

    private void recordLoginLog(SessionCreateReq req) {
        IamLoginLog loginLog = new IamLoginLog();
        loginLog.setAuthIdentifier(req.getAuthIdentifier());
        loginLog.setAuthType(req.getAuthType());
        loginLog.setLoginStatus(0);
        loginLog.setMessage("登录成功");
        loginLog.setUserCode(req.getUserCode());
        loginLog.setUsername(req.getAuthIdentifier());
        loginLog.setCreateBy(req.getAuthIdentifier());
        loginLog.setUpdateBy(req.getAuthIdentifier());

        HttpServletRequest request = RequestHelper.getRequest();
        if (request != null) {
            loginLog.setIpAddress(IpHelper.getOriginIp(request));
            loginLog.setUserAgent(request.getHeader("User-Agent"));
        }
        ssoLoginLogMapper.insertLoginLog(loginLog);
    }
}
