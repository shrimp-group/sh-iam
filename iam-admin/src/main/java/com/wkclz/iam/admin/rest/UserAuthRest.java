package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamUserAuthPasswordService;
import com.wkclz.iam.admin.service.IamUserAuthService;
import com.wkclz.iam.common.dto.IamUserAuthDto;
import com.wkclz.iam.common.entity.IamUserAuth;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Route.PREFIX)
public class UserAuthRest {

    @Autowired
    protected IamUserAuthService iamUserAuthService;
    @Resource
    private IamUserAuthPasswordService iamUserAuthPasswordService;

    @GetMapping(Route.USER_AUTH_LIST)
    public R<List<IamUserAuth>> userAuthList(IamUserAuth entity) {
        return R.ok(iamUserAuthService.selectByEntity(entity));
    }

    @GetMapping(Route.USER_AUTH_INFO)
    public R<IamUserAuth> userAuthInfo(@RequestParam Long id) {
        IamUserAuth userAuth = iamUserAuthService.selectById(id);
        return R.ok(userAuth);
    }

    @PostMapping(Route.USER_AUTH_CREATE)
    public R<IamUserAuth> userAuthCreate(@RequestBody IamUserAuthDto iamUserAuthDto) {
        return R.ok(iamUserAuthService.create(iamUserAuthDto));
    }

    @PostMapping(Route.USER_AUTH_UPDATE)
    public R<IamUserAuth> userAuthUpdate(@RequestBody IamUserAuthDto iamUserAuthDto) {
        return R.ok(iamUserAuthService.update(iamUserAuthDto));
    }

    @PostMapping(Route.USER_AUTH_REMOVE)
    public R<IamUserAuth> userAuthRemove(@RequestBody IamUserAuthDto iamUserAuthDto) {
        return R.ok(iamUserAuthService.remove(iamUserAuthDto));
    }

    @PostMapping(Route.USER_AUTH_RESET_PASSWORD)
    public R<Void> userAuthResetPassword(@RequestBody IamUserAuthDto dto) {
        Assert.notNull(dto.getUserCode(), "用户编码不能为空");
        Assert.notNull(dto.getPassword(), "新密码不能为空");
        iamUserAuthPasswordService.resetPassword(dto.getUserCode(), dto.getPassword());
        return R.ok();
    }

}