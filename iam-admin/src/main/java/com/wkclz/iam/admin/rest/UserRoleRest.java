package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.UserRoleBindReq;
import com.wkclz.iam.admin.bean.resp.UserMenuSourceResp;
import com.wkclz.iam.admin.bean.resp.UserRoleResp;
import com.wkclz.iam.admin.service.IamUserRoleService;
import com.wkclz.iam.common.dto.IamUserRoleDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping(Route.PREFIX)
@Tag(name = "用户角色管理", description = "用户-角色关联管理接口")
public class UserRoleRest {

    @Autowired
    protected IamUserRoleService iamUserRoleService;

    @GetMapping(Route.USER_ROLE_LIST)
    @Operation(summary = "查询用户角色列表")
    public R<List<UserRoleResp>> userRoleList(
            @RequestParam @NotBlank(message = "userCode 不能为空") String userCode,
            @RequestParam(required = false) String roleCode) {
        List<UserRoleResp> list = iamUserRoleService.getUserRoleList(userCode, roleCode);
        return R.ok(list);
    }

    @PostMapping(Route.USER_ROLE_BIND)
    @Operation(summary = "绑定用户角色")
    public R<Void> userRoleBind(@Valid @RequestBody UserRoleBindReq req) {
        iamUserRoleService.bind(req);
        return R.ok();
    }

    @PostMapping(Route.USER_ROLE_UNBIND)
    @Operation(summary = "解绑用户角色")
    public R<Void> userRoleUnbind(@RequestParam @NotNull(message = "id 不能为空") Long id) {
        iamUserRoleService.unbind(id);
        return R.ok();
    }

    @GetMapping(Route.USER_ROLE_ROLE_TREE)
    @Operation(summary = "查询用户角色树")
    public R<List<IamUserRoleDto>> userRoleTree(
            @RequestParam @NotBlank(message = "userCode 不能为空") String userCode,
            @RequestParam @NotBlank(message = "appCode 不能为空") String appCode) {
        List<IamUserRoleDto> tree = iamUserRoleService.getUserRoleTree(userCode, appCode);
        return R.ok(tree);
    }

    @GetMapping(Route.USER_MENU_SOURCE_LIST)
    @Operation(summary = "查询用户菜单来源列表")
    public R<List<UserMenuSourceResp>> userMenuSourceList(
            @RequestParam @NotBlank(message = "userCode 不能为空") String userCode,
            @RequestParam(required = false) String appCode) {
        List<UserMenuSourceResp> list = iamUserRoleService.getUserMenuSourceList(userCode, appCode);
        return R.ok(list);
    }

}
