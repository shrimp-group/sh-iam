package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.resp.RoleBoundResp;
import com.wkclz.iam.admin.service.IamRoleMenuService;
import com.wkclz.iam.admin.bean.req.RoleMenuSaveReq;
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
    public R roleMenuList(String roleCode) {
        Assert.notNull(roleCode, "roleCode 不能为空!");
        List<String> menuCodes = iamRoleMenuService.getBoundMenuCodes(roleCode);
        return R.ok(menuCodes);
    }

    @PostMapping(Route.ROLE_MENU_SAVE)
    public R roleMenuSave(@RequestBody RoleMenuSaveReq req) {
        Assert.notNull(req.getRoleCode(), "roleCode 不能为空!");
        iamRoleMenuService.saveRoleMenus(req.getRoleCode(), req.getMenuCodes());
        return R.ok();
    }

    @GetMapping(Route.ROLE_MENU_BOUND_ROLES)
    public R roleMenuBoundRoles(String menuCode) {
        Assert.notNull(menuCode, "menuCode 不能为空!");
        List<RoleBoundResp> list = iamRoleMenuService.getBoundRoles(menuCode);
        return R.ok(list);
    }

}
