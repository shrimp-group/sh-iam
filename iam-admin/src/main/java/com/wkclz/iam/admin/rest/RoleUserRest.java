package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.RoleUserBindReq;
import com.wkclz.iam.admin.bean.req.RoleUserPageReq;
import com.wkclz.iam.admin.bean.req.RoleUserUnbindReq;
import com.wkclz.iam.admin.bean.resp.RoleUserResp;
import com.wkclz.iam.admin.service.IamUserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping(Route.PREFIX)
@Tag(name = "角色用户管理", description = "角色-用户关联管理接口")
public class RoleUserRest {

    @Autowired
    protected IamUserRoleService iamUserRoleService;

    @GetMapping(Route.ROLE_USER_PAGE)
    @Operation(summary = "分页查询角色下用户")
    public R<PageData<RoleUserResp>> roleUserPage(@Valid RoleUserPageReq req) {
        PageData<RoleUserResp> page = iamUserRoleService.getRoleUserPage(req);
        return R.ok(page);
    }

    @PostMapping(Route.ROLE_USER_BIND)
    @Operation(summary = "批量绑定角色用户")
    public R<Void> roleUserBind(@Valid @RequestBody RoleUserBindReq req) {
        iamUserRoleService.batchBind(req);
        return R.ok();
    }

    @PostMapping(Route.ROLE_USER_UNBIND)
    @Operation(summary = "批量解绑角色用户")
    public R<Void> roleUserUnbind(@Valid @RequestBody RoleUserUnbindReq req) {
        iamUserRoleService.batchUnbind(req.getIds());
        return R.ok();
    }

}
