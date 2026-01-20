package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamRoleMenuService;
import com.wkclz.iam.common.entity.IamRoleMenu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Route.PREFIX)
public class RoleMenuRest {

    @Autowired
    protected IamRoleMenuService iamRoleMenuService;

    @GetMapping(Route.ROLE_MENU_LIST)
    public R roleMenuList(IamRoleMenu entity) {
        Assert.notNull(entity.getRoleCode(), "roleCode 不能为空!");
        List<IamRoleMenu> iamRoleMenus = iamRoleMenuService.selectByEntity(entity);
        return R.ok(iamRoleMenus);
    }

    @PostMapping(Route.ROLE_MENU_BIND)
    public R roleMenuBind(@RequestBody IamRoleMenu entity) {
        Assert.notNull(entity.getRoleCode(), "roleCode 不能为空!");
        Assert.notNull(entity.getMenuCode(), "menuCode 不能为空!");
        int insert = iamRoleMenuService.insert(entity);
        return R.ok(insert);
    }

    @PostMapping(Route.ROLE_MENU_UNBIND)
    public R roleMenuUnbind(@RequestBody IamRoleMenu entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        IamRoleMenu remove = iamRoleMenuService.remove(entity);
        return R.ok(remove);
    }

}