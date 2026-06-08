package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamApiFieldService;
import com.wkclz.iam.common.dto.IamApiFieldDto;
import com.wkclz.iam.common.entity.IamApiField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API字段权限管理
 */
@RestController
@RequestMapping(Route.PREFIX)
public class ApiFieldRest {

    @Autowired
    private IamApiFieldService iamApiFieldService;

    @GetMapping(Route.API_FIELD_PAGE)
    public R apiFieldPage(IamApiFieldDto entity) {
        PageData<IamApiFieldDto> page = iamApiFieldService.getApiFieldPage(entity);
        return R.ok(page);
    }

    @GetMapping(Route.API_FIELD_INFO)
    public R apiFieldInfo(IamApiField entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        IamApiField apiField = iamApiFieldService.selectById(entity.getId());
        return R.ok(apiField);
    }

    @GetMapping(Route.API_FIELD_LIST_BY_API)
    public R apiFieldListByApi(IamApiField entity) {
        Assert.notNull(entity.getApiCode(), "apiCode 不能为空");
        List<IamApiField> list = iamApiFieldService.listByApi(entity.getApiCode());
        return R.ok(list);
    }

    @PostMapping(Route.API_FIELD_CREATE)
    public R apiFieldCreate(@RequestBody IamApiField entity) {
        paramCheck(entity);
        entity = iamApiFieldService.create(entity);
        return R.ok(entity);
    }

    @PostMapping(Route.API_FIELD_UPDATE)
    public R apiFieldUpdate(@RequestBody IamApiField entity) {
        paramCheck(entity);
        entity = iamApiFieldService.update(entity);
        return R.ok(entity);
    }

    @PostMapping(Route.API_FIELD_REMOVE)
    public R apiFieldRemove(@RequestBody IamApiField entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        entity = iamApiFieldService.remove(entity);
        return R.ok(entity);
    }

    private void paramCheck(IamApiField entity) {
        if (entity.getId() != null) {
            Assert.notNull(entity.getVersion(), "version 不能为空");
        }
        Assert.notNull(entity.getAppCode(), "appCode 不能为空");
        Assert.notNull(entity.getApiCode(), "apiCode 不能为空");
        Assert.notNull(entity.getFieldName(), "fieldName 不能为空");
        Assert.notNull(entity.getAction(), "action 不能为空");
    }

}
