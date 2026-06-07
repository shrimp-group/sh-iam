package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.RoleUserBindReq;
import com.wkclz.iam.admin.bean.req.RoleUserPageReq;
import com.wkclz.iam.admin.bean.req.RoleUserUnbindReq;
import com.wkclz.iam.admin.bean.resp.RoleUserResp;
import com.wkclz.iam.admin.service.IamUserRoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Route.PREFIX)
public class RoleUserRest {

    @Autowired
    protected IamUserRoleService iamUserRoleService;

    @GetMapping(Route.ROLE_USER_PAGE)
    public R<PageData<RoleUserResp>> roleUserPage(RoleUserPageReq req) {
        PageData<RoleUserResp> page = iamUserRoleService.getRoleUserPage(req);
        return R.ok(page);
    }

    @PostMapping(Route.ROLE_USER_BIND)
    public R<Void> roleUserBind(@Valid @RequestBody RoleUserBindReq req) {
        Assert.notNull(req.getStartTime(), "startTime 不能为空!");
        Assert.notNull(req.getEndTime(), "endTime 不能为空!");
        iamUserRoleService.batchBind(req);
        return R.ok();
    }

    @PostMapping(Route.ROLE_USER_UNBIND)
    public R<Void> roleUserUnbind(@Valid @RequestBody RoleUserUnbindReq req) {
        iamUserRoleService.batchUnbind(req.getIds());
        return R.ok();
    }

}
