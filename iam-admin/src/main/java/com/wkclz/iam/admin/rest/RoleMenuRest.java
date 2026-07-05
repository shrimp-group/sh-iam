package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.RoleMenuSaveReq;
import com.wkclz.iam.admin.bean.resp.RoleBoundResp;
import com.wkclz.iam.admin.service.IamRoleMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping(Route.PREFIX)
@Tag(name = "角色菜单管理", description = "角色-菜单关联管理接口")
public class RoleMenuRest {

    @Autowired
    protected IamRoleMenuService iamRoleMenuService;

    @GetMapping(Route.ROLE_MENU_LIST)
    @Operation(summary = "查询角色已绑定的菜单编码列表")
    public R<List<String>> roleMenuList(@RequestParam @NotBlank(message = "roleCode 不能为空") String roleCode) {
        List<String> menuCodes = iamRoleMenuService.getBoundMenuCodes(roleCode);
        return R.ok(menuCodes);
    }

    @PostMapping(Route.ROLE_MENU_SAVE)
    @Operation(summary = "批量保存角色-菜单绑定关系")
    public R<Void> roleMenuSave(@Valid @RequestBody RoleMenuSaveReq req) {
        iamRoleMenuService.saveRoleMenus(req.getRoleCode(), req.getMenuCodes());
        return R.ok();
    }

    @GetMapping(Route.ROLE_MENU_BOUND_ROLES)
    @Operation(summary = "查询菜单已绑定的角色列表")
    public R<List<RoleBoundResp>> roleMenuBoundRoles(@RequestParam @NotBlank(message = "menuCode 不能为空") String menuCode) {
        List<RoleBoundResp> list = iamRoleMenuService.getBoundRoles(menuCode);
        return R.ok(list);
    }

}
