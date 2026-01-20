package com.wkclz.iam.sso.rest;


import com.wkclz.core.base.R;
import com.wkclz.iam.sdk.model.RegisterRequest;
import com.wkclz.iam.sso.Route;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Route.PREFIX)
public class RegisterRest {

    @PostMapping(Route.PUBLIC_SSO_REGISTER)
    public R publicSsoRegister(@RequestBody RegisterRequest registerRequest) {
        return R.ok();
    }

}
