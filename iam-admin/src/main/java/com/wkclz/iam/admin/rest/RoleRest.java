package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.RoleCreateReq;
import com.wkclz.iam.admin.bean.req.RoleListReq;
import com.wkclz.iam.admin.bean.req.RoleUpdateReq;
import com.wkclz.iam.admin.bean.resp.RoleResp;
import com.wkclz.iam.admin.service.IamRoleService;
import com.wkclz.iam.common.dto.IamRoleDto;
import com.wkclz.iam.common.entity.IamRole;
import com.wkclz.tool.utils.BeanUtil;
import com.wkclz.web.bean.IdReq;
import com.wkclz.web.bean.RemoveReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping(Route.PREFIX)
@Tag(name = "角色管理", description = "角色管理接口")
public class RoleRest {

    @Autowired
    protected IamRoleService iamRoleService;

    @GetMapping(Route.ROLE_LIST)
    @Operation(summary = "角色列表")
    public R<List<RoleResp>> roleList(@Valid RoleListReq req) {
        IamRole entity = BeanUtil.cp(req, IamRole.class);
        List<IamRoleDto> list = iamRoleService.roleList(entity);
        return R.ok(BeanUtil.cp(list, RoleResp.class));
    }

    @GetMapping(Route.ROLE_INFO)
    @Operation(summary = "角色详情")
    public R<RoleResp> roleInfo(@Valid IdReq req) {
        IamRole role = iamRoleService.selectById(req.getId());
        return R.ok(BeanUtil.cp(role, RoleResp.class));
    }

    @PostMapping(Route.ROLE_CREATE)
    @Operation(summary = "角色创建")
    public R<RoleResp> roleCreate(@Valid @RequestBody RoleCreateReq req) {
        IamRole entity = BeanUtil.cp(req, IamRole.class);
        entity = iamRoleService.create(entity);
        return R.ok(BeanUtil.cp(entity, RoleResp.class));
    }

    @PostMapping(Route.ROLE_UPDATE)
    @Operation(summary = "角色更新")
    public R<RoleResp> roleUpdate(@Valid @RequestBody RoleUpdateReq req) {
        IamRole entity = BeanUtil.cp(req, IamRole.class);
        entity = iamRoleService.update(entity);
        return R.ok(BeanUtil.cp(entity, RoleResp.class));
    }

    @PostMapping(Route.ROLE_REMOVE)
    @Operation(summary = "角色删除")
    public R<Void> roleRemove(@Valid @RequestBody RemoveReq req) {
        if (req.getId() != null) {
            IamRole entity = new IamRole();
            entity.setId(req.getId());
            iamRoleService.remove(entity);
        } else if (req.getIds() != null) {
            for (Long id : req.getIds()) {
                IamRole entity = new IamRole();
                entity.setId(id);
                iamRoleService.remove(entity);
            }
        }
        return R.ok();
    }

    @GetMapping(Route.ROLE_TREE)
    @Operation(summary = "角色树")
    public R<List<RoleResp>> roleTree(@Valid RoleListReq req) {
        IamRole entity = BeanUtil.cp(req, IamRole.class);
        List<IamRoleDto> tree = iamRoleService.roleTree(entity);
        return R.ok(BeanUtil.cp(tree, RoleResp.class));
    }

}
