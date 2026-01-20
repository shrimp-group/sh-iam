package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamApiService;
import com.wkclz.iam.common.entity.IamApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Route.PREFIX)
public class ApiRest {

    @Autowired
    protected IamApiService iamApiService;

    @GetMapping(Route.API_PAGE)
    public R apiPage(IamApi entity) {
        PageData<IamApi> page = iamApiService.selectPage(entity);
        return R.ok(page);
    }

    @GetMapping(Route.API_INFO)
    public R apiInfo(IamApi entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        IamApi api = iamApiService.selectById(entity.getId());
        return R.ok(api);
    }

    @PostMapping(Route.API_CREATE)
    public R apiCreate(@RequestBody IamApi entity) {
        paramCheck(entity);
        entity = iamApiService.create(entity);
        return R.ok(entity);
    }

    @PostMapping(Route.API_UPDATE)
    public R apiUpdate(@RequestBody IamApi entity) {
        paramCheck(entity);
        entity = iamApiService.update(entity);
        return R.ok(entity);
    }

    @PostMapping(Route.API_REMOVE)
    public R apiRemove(@RequestBody IamApi entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        entity = iamApiService.remove(entity);
        return R.ok(entity);
    }

    @PostMapping(Route.API_SYNC)
    public R apiSync() {
        iamApiService.syncApi();
        return R.ok();
    }

    private void paramCheck(IamApi entity) {
        if (entity.getId() != null) {
            Assert.notNull(entity.getVersion(), "version 不能为空");
        }
        Assert.notNull(entity.getApiUri(), "apiUri 不能为空");
        Assert.notNull(entity.getApiMethod(), "apiMethod 不能为空");
        Assert.notNull(entity.getAppCode(), "appCode 不能为空");
    }

}