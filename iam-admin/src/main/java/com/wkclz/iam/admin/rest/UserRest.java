package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamUserService;
import com.wkclz.iam.common.dto.IamUserDto;
import com.wkclz.iam.common.entity.IamUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Route.PREFIX)
public class UserRest {

    @Autowired
    protected IamUserService iamUserService;

    @GetMapping(Route.USER_PAGE)
    public R userPage(IamUser entity) {
        PageData<IamUser> page = iamUserService.userPage(entity);
        return R.ok(page);
    }

    @GetMapping(Route.USER_INFO)
    public R userInfo(IamUser entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        IamUser user = iamUserService.selectById(entity.getId());
        return R.ok(user);
    }

    @PostMapping(Route.USER_CREATE)
    public R userCreate(@RequestBody IamUserDto dto) {
        paramCheck(dto);
        Assert.notNull(dto.getPassword(), "密码不能为空");
        dto = iamUserService.customCreate(dto);
        dto.setPassword(null);
        return R.ok(dto);
    }

    @PostMapping(Route.USER_UPDATE)
    public R userUpdate(@RequestBody IamUser entity) {
        paramCheck(entity);
        entity = iamUserService.update(entity);
        return R.ok(entity);
    }

    @PostMapping(Route.USER_REMOVE)
    public R userRemove(@RequestBody IamUser entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        entity = iamUserService.remove(entity);
        return R.ok(entity);
    }

    private void paramCheck(IamUser entity) {
        if (entity.getId() != null) {
            Assert.notNull(entity.getVersion(), ResultCode.UPDATE_NO_VERSION.getMessage());
        }
        Assert.notNull(entity.getUsername(), "用户名不能为空");
        Assert.notNull(entity.getNickname(), "昵称不能为空");
    }

}
