package com.wkclz.iam.sso.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.sdk.model.LoginRequest;
import com.wkclz.iam.sdk.model.LoginResponse;
import com.wkclz.iam.sso.Route;
import com.wkclz.iam.sso.service.IamLoginService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Route.PREFIX)
public class LoginRest {


    // 假设存在这些服务类
    @Autowired
    private IamLoginService iamLoginService;

    @PostMapping(Route.PUBLIC_SSO_LOGIN)
    public R publicSsoLogin(HttpServletRequest request, @RequestBody LoginRequest loginRequest) {
        Assert.notNull(loginRequest.getUsername(), "用户名不能为空!");
        Assert.notNull(loginRequest.getPassword(), "密码不能为空!");
        LoginResponse response = iamLoginService.loginByUsernameAndPassword(request, loginRequest);
        return R.ok(response);
    }

    @GetMapping(Route.PUBLIC_SSO_LOGOUT)
    public R publicSsoLogout(HttpServletRequest request) {
        iamLoginService.logout(request);
        return R.ok();
    }


}
