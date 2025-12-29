package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamUserService;
import com.wkclz.iam.common.dto.IamUserDto;
import com.wkclz.iam.common.entity.IamUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserRest {

    @Autowired
    protected IamUserService iamUserService;


    @GetMapping(Route.USER_PAGE)
    public R userPage(IamUser entity) {
        PageData<IamUser> page = iamUserService.userPage(entity);
        return R.ok(page);
    }

    @PostMapping(Route.USER_CREATE)
    public R userCreate(@RequestBody IamUserDto iamUserDto) {
        return R.ok(iamUserService.userCreate(iamUserDto));
    }


    @PostMapping(Route.USER_UPDATE)
    public R userUpdate(@RequestBody IamUserDto iamUserDto) {
        return R.ok(iamUserService.userCreate(iamUserDto));
    }

    @PostMapping(Route.USER_REMOVE)
    public R userRemove(@RequestBody IamUserDto iamUserDto) {
        return R.ok(iamUserService.userCreate(iamUserDto));
    }


}
