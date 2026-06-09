package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamRoleService;
import com.wkclz.iam.common.dto.IamRoleDto;
import com.wkclz.iam.common.entity.IamRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Route.PREFIX)
public class RoleRest {

    @Autowired
    protected IamRoleService iamRoleService;

    @GetMapping(Route.ROLE_LIST)
    public R<List<IamRoleDto>> roleList(IamRole entity) {
        Assert.notNull(entity.getAppCode(), "appCode 不能为空!");
        List<IamRoleDto> iamRoles = iamRoleService.roleList(entity);
        return R.ok(iamRoles);
    }

    @GetMapping(Route.ROLE_INFO)
    public R<IamRole> roleInfo(IamRole entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        IamRole role = iamRoleService.selectById(entity.getId());
        return R.ok(role);
    }

    @PostMapping(Route.ROLE_CREATE)
    public R<IamRole> roleCreate(@RequestBody IamRole entity) {
        paramCheck(entity);
        entity = iamRoleService.create(entity);
        return R.ok(entity);
    }

    @PostMapping(Route.ROLE_UPDATE)
    public R<IamRole> roleUpdate(@RequestBody IamRole entity) {
        paramCheck(entity);
        entity = iamRoleService.update(entity);
        return R.ok(entity);
    }

    @PostMapping(Route.ROLE_REMOVE)
    public R<IamRole> roleRemove(@RequestBody IamRole entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        // 检查角色下是否有子角色
        IamRole param = new IamRole();
        param.setParentCode(entity.getRoleCode());
        long childrenRoleCount = iamRoleService.selectCountByEntity(param);
        if (childrenRoleCount > 0) {
            return R.error("请先删除子角色");
        }
        entity = iamRoleService.remove(entity);
        return R.ok(entity);
    }

    @GetMapping(Route.ROLE_TREE)
    public R<List<IamRoleDto>> roleTree(IamRole entity) {
        Assert.notNull(entity.getAppCode(), "appCode 不能为空!");
        List<IamRoleDto> tree = iamRoleService.roleTree(entity);
        return R.ok(tree);
    }

    private void paramCheck(IamRole entity) {
        if (entity.getId() != null) {
            Assert.notNull(entity.getVersion(), "version 不能为空");
        }
        Assert.notNull(entity.getAppCode(), "appCode 不能为空");
        Assert.notNull(entity.getRoleName(), "roleName 不能为空");
    }

}