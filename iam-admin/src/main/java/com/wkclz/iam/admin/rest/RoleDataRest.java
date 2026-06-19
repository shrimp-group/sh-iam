package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.RoleDataBindReq;
import com.wkclz.iam.admin.bean.req.RoleDataListReq;
import com.wkclz.iam.admin.bean.resp.RoleDataResp;
import com.wkclz.iam.admin.service.IamRoleDataService;
import com.wkclz.iam.common.entity.IamRoleData;
import com.wkclz.tool.utils.BeanUtil;
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
@Tag(name = "角色数据权限", description = "角色-数据权限关联管理接口")
public class RoleDataRest {

    @Autowired
    protected IamRoleDataService iamRoleDataService;

    @GetMapping(Route.ROLE_DATA_LIST)
    @Operation(summary = "查询角色数据权限列表")
    public R<List<RoleDataResp>> roleDataList(@Valid RoleDataListReq req) {
        List<IamRoleData> list = iamRoleDataService.getRoleDataList(req.getRoleCode(), req.getDimensionCode());
        return R.ok(BeanUtil.cp(list, RoleDataResp.class));
    }

    @PostMapping(Route.ROLE_DATA_BIND)
    @Operation(summary = "角色数据权限绑定")
    public R<RoleDataResp> roleDataBind(@Valid @RequestBody RoleDataBindReq req) {
        IamRoleData entity = BeanUtil.cp(req, IamRoleData.class);
        entity = iamRoleDataService.create(entity);
        return R.ok(BeanUtil.cp(entity, RoleDataResp.class));
    }

    @PostMapping(Route.ROLE_DATA_UNBIND)
    @Operation(summary = "角色数据权限解绑")
    public R<Void> roleDataUnbind(@Valid @RequestBody RemoveReq req) {
        IamRoleData entity = new IamRoleData();
        entity.setId(req.getId());
        iamRoleDataService.remove(entity);
        return R.ok();
    }

}
