package com.wkclz.iam.sso.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.sdk.bean.req.LoginReq;
import com.wkclz.iam.contract.bean.resp.LoginResp;
import com.wkclz.iam.sso.Route;
import com.wkclz.iam.sso.service.IamLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Route.PREFIX)
@Validated
@Tag(name = "SSO登录", description = "SSO登录认证接口")
public class LoginRest {

    @Autowired
    private IamLoginService iamLoginService;

    @PostMapping(Route.PUBLIC_SSO_LOGIN)
    @Operation(summary = "用户登录")
    public R<LoginResp> publicSsoLogin(HttpServletRequest request, @Valid @RequestBody LoginReq loginReq) {
        LoginResp response = iamLoginService.loginByUsernameAndPassword(request, loginReq);
        return R.ok(response);
    }

    @GetMapping(Route.PUBLIC_SSO_LOGOUT)
    @Operation(summary = "用户登出")
    public R<Void> publicSsoLogout(HttpServletRequest request) {
        iamLoginService.logout(request);
        return R.ok();
    }

}
