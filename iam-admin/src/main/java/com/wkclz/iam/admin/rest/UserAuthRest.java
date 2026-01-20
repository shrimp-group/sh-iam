package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamUserAuthService;
import com.wkclz.iam.common.dto.IamUserAuthDto;
import com.wkclz.iam.common.entity.IamUserAuth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Route.PREFIX)
public class UserAuthRest {

    @Autowired
    protected IamUserAuthService iamUserAuthService;

    @GetMapping(Route.USER_AUTH_LIST)
    public R userAuthList(IamUserAuth entity) {
        return R.ok(iamUserAuthService.selectByEntity(entity));
    }

    @GetMapping(Route.USER_AUTH_INFO)
    public R userAuthInfo(@RequestParam Long id) {
        IamUserAuth userAuth = iamUserAuthService.selectById(id);
        return R.ok(userAuth);
    }

    @PostMapping(Route.USER_AUTH_CREATE)
    public R userAuthCreate(@RequestBody IamUserAuthDto iamUserAuthDto) {
        return R.ok(iamUserAuthService.create(iamUserAuthDto));
    }

    @PostMapping(Route.USER_AUTH_UPDATE)
    public R userAuthUpdate(@RequestBody IamUserAuthDto iamUserAuthDto) {
        return R.ok(iamUserAuthService.update(iamUserAuthDto));
    }

    @PostMapping(Route.USER_AUTH_REMOVE)
    public R userAuthRemove(@RequestBody IamUserAuthDto iamUserAuthDto) {
        return R.ok(iamUserAuthService.remove(iamUserAuthDto));
    }

}