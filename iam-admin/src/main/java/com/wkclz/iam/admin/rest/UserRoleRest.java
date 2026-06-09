package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.UserRoleBindReq;
import com.wkclz.iam.admin.bean.resp.UserMenuSourceResp;
import com.wkclz.iam.admin.bean.resp.UserRoleResp;
import com.wkclz.iam.admin.service.IamUserRoleService;
import com.wkclz.iam.common.dto.IamUserRoleDto;
import com.wkclz.iam.common.entity.IamUserRole;
import jakarta.validation.Valid;
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
    public R<List<UserRoleResp>> userRoleList(@RequestParam String userCode,
                                              @RequestParam(required = false) String roleCode) {
        Assert.notNull(userCode, "userCode 不能为空!");
        List<UserRoleResp> list = iamUserRoleService.getUserRoleList(userCode, roleCode);
        return R.ok(list);
    }

    @PostMapping(Route.USER_ROLE_BIND)
    public R<IamUserRole> userRoleBind(@Valid @RequestBody UserRoleBindReq req) {
        IamUserRole entity = iamUserRoleService.bind(req);
        return R.ok(entity);
    }

    @PostMapping(Route.USER_ROLE_UNBIND)
    public R<IamUserRole> userRoleUnbind(@RequestParam Long id) {
        IamUserRole entity = iamUserRoleService.unbind(id);
        return R.ok(entity);
    }

    @GetMapping(Route.USER_ROLE_ROLE_TREE)
    public R<List<IamUserRoleDto>> userRoleTree(@RequestParam String userCode,
                                                @RequestParam String appCode) {
        List<IamUserRoleDto> tree = iamUserRoleService.getUserRoleTree(userCode, appCode);
        return R.ok(tree);
    }

    @GetMapping(Route.USER_MENU_SOURCE_LIST)
    public R<List<UserMenuSourceResp>> userMenuSourceList(
            @RequestParam String userCode,
            @RequestParam(required = false) String appCode) {
        Assert.notNull(userCode, "userCode 不能为空!");
        List<UserMenuSourceResp> list = iamUserRoleService.getUserMenuSourceList(userCode, appCode);
        return R.ok(list);
    }

}
