package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.init.RestfulScan;
import com.wkclz.iam.admin.service.IamApiService;
import com.wkclz.iam.common.entity.IamApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Route.PREFIX)
public class ApiRest {

    @Autowired
    private RestfulScan restfulScan;
    @Autowired
    protected IamApiService iamApiService;

    @GetMapping(Route.API_PAGE)
    public R apiPage(IamApi entity) {
        PageData<IamApi> page = iamApiService.getApiPage(entity);
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

    @GetMapping(Route.API_OPTIONS)
    public R apiOptions(IamApi entity) {
        List<IamApi> list = iamApiService.getApiOptions(entity);
        return R.ok(list);
    }

    @PostMapping(Route.API_SYNC)
    public R apiSync() {
        restfulScan.run(null);
        return R.ok();
    }

    @GetMapping(Route.API_COPY)
    public R apiCopy(IamApi entity) {
        List<IamApi> list = iamApiService.getApis4Copy(entity);
        return R.ok(list);
    }
    @PostMapping(Route.API_PASTE)
    public R apiPaste(@RequestBody List<IamApi> entity) {
        Integer count = iamApiService.apiPaste(entity);
        return R.ok(count);
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