package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamRoleDataService;
import com.wkclz.iam.common.entity.IamRoleData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Route.PREFIX)
public class RoleDataRest {

    @Autowired
    protected IamRoleDataService iamRoleDataService;

    @GetMapping(Route.ROLE_DATA_LIST)
    public R roleDataList(IamRoleData entity) {
        Assert.notNull(entity.getRoleCode(), "roleCode 不能为空!");
        Assert.notNull(entity.getRoleCode(), "dimensionCode 不能为空!");
        List<IamRoleData> list = iamRoleDataService.getRoleDataList(entity.getRoleCode(), entity.getDimensionCode());
        return R.ok(list);
    }

    @PostMapping(Route.ROLE_DATA_BIND)
    public R roleDataBind(@RequestBody IamRoleData entity) {
        paramCheck(entity);
        entity = iamRoleDataService.create(entity);
        return R.ok(entity);
    }

    @PostMapping(Route.ROLE_DATA_UNBIND)
    public R roleDataUnbind(@RequestBody IamRoleData entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        entity = iamRoleDataService.remove(entity);
        return R.ok(entity);
    }

    private void paramCheck(IamRoleData entity) {
        Assert.notNull(entity.getAppCode(), "appCode 不能为空");
        Assert.notNull(entity.getRoleCode(), "menuCode 不能为空");
        Assert.notNull(entity.getDimensionCode(), "dimensionCode 不能为空");
        Assert.notNull(entity.getDataCode(), "apiCode 不能为空");
        if (entity.getId() == null) {
            // 创建操作的参数检查
        } else {
            // 更新操作的参数检查
            Assert.notNull(entity.getVersion(), "version 不能为空");
        }

    }

}
