package com.wkclz.iam.sso.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.sso.bean.req.RegisterReq;
import com.wkclz.iam.sso.Route;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(Route.PREFIX)
@Validated
@Tag(name = "用户注册", description = "用户注册接口")
public class RegisterRest {

    @PostMapping(Route.PUBLIC_SSO_REGISTER)
    @Operation(summary = "用户注册")
    public R<Void> publicSsoRegister(@Valid @RequestBody RegisterReq registerReq) {
        return R.ok();
    }

}
