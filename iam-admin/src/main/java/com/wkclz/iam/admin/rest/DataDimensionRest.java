package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamDataDimensionService;
import com.wkclz.iam.common.entity.IamDataDimension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Route.PREFIX)
public class DataDimensionRest {


    @Autowired
    protected IamDataDimensionService iamDataDimensionService;

    @GetMapping(Route.DATA_DIM_PAGE)
    public R dataDimPage(IamDataDimension entity) {
        PageData<IamDataDimension> page = iamDataDimensionService.getDataDimensionPage(entity);
        return R.ok(page);
    }

    @GetMapping(Route.DATA_DIM_INFO)
    public R dataDimInfo(IamDataDimension entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        IamDataDimension api = iamDataDimensionService.selectById(entity.getId());
        return R.ok(api);
    }

    @PostMapping(Route.DATA_DIM_CREATE)
    public R dataDimCreate(@RequestBody IamDataDimension entity) {
        paramCheck(entity);
        entity = iamDataDimensionService.create(entity);
        return R.ok(entity);
    }

    @PostMapping(Route.DATA_DIM_UPDATE)
    public R dataDimUpdate(@RequestBody IamDataDimension entity) {
        paramCheck(entity);
        entity = iamDataDimensionService.update(entity);
        return R.ok(entity);
    }

    @PostMapping(Route.DATA_DIM_REMOVE)
    public R dataDimRemove(@RequestBody IamDataDimension entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        entity = iamDataDimensionService.remove(entity);
        return R.ok(entity);
    }


    private void paramCheck(IamDataDimension entity) {
        if (entity.getId() != null) {
            Assert.notNull(entity.getVersion(), "version 不能为空");
        }
        Assert.notNull(entity.getDimensionName(), "dimensionName 不能为空");
        Assert.notNull(entity.getAppCode(), "appCode 不能为空");
    }

}