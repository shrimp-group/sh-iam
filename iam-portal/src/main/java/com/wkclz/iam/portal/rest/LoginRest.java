package com.wkclz.iam.portal.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.portal.Route;
import com.wkclz.iam.portal.bean.req.LoginReq;
import com.wkclz.iam.portal.bean.resp.SsoAppInfoResp;
import com.wkclz.iam.session.bean.resp.LoginResp;
import com.wkclz.iam.portal.service.PasswordLoginService;
import com.wkclz.iam.portal.service.SsoAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Route.PREFIX)
@Validated
@Tag(name = "SSO登录", description = "SSO登录认证接口")
public class LoginRest {

    @Autowired
    private SsoAppService ssoAppService;
    @Autowired
    private PasswordLoginService passwordLoginService;

    @PostMapping(Route.PUBLIC_SSO_LOGIN)
    @Operation(summary = "用户登录")
    public R<LoginResp> publicSsoLogin(HttpServletRequest request, @Valid @RequestBody LoginReq loginReq) {
        LoginResp response = passwordLoginService.login(request, loginReq);
        return R.ok(response);
    }

    @GetMapping(Route.PUBLIC_SSO_LOGOUT)
    @Operation(summary = "用户登出")
    public R<Void> publicSsoLogout() {
        passwordLoginService.logout();
        return R.ok();
    }

    @GetMapping(Route.PUBLIC_SSO_APP_INFO)
    @Operation(summary = "登录页应用信息查询", description = "公开接口，仅返回应用名称与登录页背景图")
    public R<SsoAppInfoResp> publicSsoAppInfo(@RequestParam @NotBlank(message = "appCode 不能为空") String appCode) {
        return R.ok(ssoAppService.getAppInfo(appCode));
    }

}
