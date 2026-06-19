package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.ApiFieldCreateReq;
import com.wkclz.iam.admin.bean.req.ApiFieldUpdateReq;
import com.wkclz.iam.admin.bean.resp.ApiFieldResp;
import com.wkclz.iam.admin.service.IamApiFieldService;
import com.wkclz.iam.common.entity.IamApiField;
import com.wkclz.tool.utils.BeanUtil;
import com.wkclz.web.bean.RemoveReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API字段权限管理
 */
@RestController
@RequestMapping(Route.PREFIX)
@Validated
@Tag(name = "API字段权限", description = "API字段权限管理接口")
public class ApiFieldRest {

    @Autowired
    private IamApiFieldService iamApiFieldService;

    @GetMapping(Route.API_FIELD_LIST_BY_API)
    @Operation(summary = "按API编码查询字段权限列表")
    public R<List<ApiFieldResp>> apiFieldListByApi(@RequestParam @NotBlank(message = "apiCode 不能为空") String apiCode) {
        List<IamApiField> list = iamApiFieldService.listByApi(apiCode);
        List<ApiFieldResp> respList = list.stream()
                .map(item -> BeanUtil.cp(item, ApiFieldResp.class))
                .toList();
        return R.ok(respList);
    }

    @PostMapping(Route.API_FIELD_CREATE)
    @Operation(summary = "创建API字段权限")
    public R<ApiFieldResp> apiFieldCreate(@Valid @RequestBody ApiFieldCreateReq req) {
        IamApiField entity = BeanUtil.cp(req, IamApiField.class);
        entity = iamApiFieldService.create(entity);
        return R.ok(BeanUtil.cp(entity, ApiFieldResp.class));
    }

    @PostMapping(Route.API_FIELD_UPDATE)
    @Operation(summary = "更新API字段权限")
    public R<ApiFieldResp> apiFieldUpdate(@Valid @RequestBody ApiFieldUpdateReq req) {
        IamApiField entity = BeanUtil.cp(req, IamApiField.class);
        entity = iamApiFieldService.update(entity);
        return R.ok(BeanUtil.cp(entity, ApiFieldResp.class));
    }

    @PostMapping(Route.API_FIELD_REMOVE)
    @Operation(summary = "删除API字段权限")
    public R<Void> apiFieldRemove(@Valid @RequestBody RemoveReq req) {
        IamApiField entity = new IamApiField();
        entity.setId(req.getId());
        iamApiFieldService.remove(entity);
        return R.ok();
    }

}
