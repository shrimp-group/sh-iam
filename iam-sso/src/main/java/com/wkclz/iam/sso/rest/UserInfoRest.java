package com.wkclz.iam.sso.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.sdk.helper.SessionHelper;
import com.wkclz.iam.sdk.model.UserSession;
import com.wkclz.iam.sso.Route;
import com.wkclz.iam.sso.service.IamLoginService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserInfoRest {


    // 假设存在这些服务类
    @Autowired
    private IamLoginService iamLoginService;

    @GetMapping(Route.USER_INFO)
    public R publicSsoLogin(HttpServletRequest request) {
        UserSession userSession = SessionHelper.getUserSession(request);
        return R.ok(userSession);
    }

}
