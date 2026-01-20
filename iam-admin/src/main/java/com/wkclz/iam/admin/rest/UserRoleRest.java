package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamUserRoleService;
import com.wkclz.iam.common.entity.IamUserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Route.PREFIX)
public class UserRoleRest {

    @Autowired
    protected IamUserRoleService iamUserRoleService;

    @GetMapping(Route.USER_ROLE_LIST)
    public R userRoleList(IamUserRole entity) {
        Assert.notNull(entity.getUserCode(), "userCode 不能为空!");
        List<IamUserRole> iamUserRoles = iamUserRoleService.selectByEntity(entity);
        return R.ok(iamUserRoles);
    }

    @PostMapping(Route.USER_ROLE_BIND)
    public R userRoleBind(@RequestBody IamUserRole entity) {
        Assert.notNull(entity.getUserCode(), "userCode 不能为空!");
        Assert.notNull(entity.getRoleCode(), "roleCode 不能为空!");
        int insert = iamUserRoleService.insert(entity);
        return R.ok(insert);
    }

    @PostMapping(Route.USER_ROLE_UNBIND)
    public R userRoleUnbind(@RequestBody IamUserRole entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        IamUserRole remove = iamUserRoleService.remove(entity);
        return R.ok(remove);
    }

}