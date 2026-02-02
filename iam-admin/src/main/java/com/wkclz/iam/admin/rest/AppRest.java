package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamAppService;
import com.wkclz.iam.common.entity.IamApp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Route.PREFIX)
public class AppRest {

    @Autowired
    protected IamAppService iamAppService;

    @GetMapping(Route.APP_PAGE)
    public R appPage(IamApp entity) {
        PageData<IamApp> page = iamAppService.getAppPage(entity);
        return R.ok(page);
    }

    @GetMapping(Route.APP_INFO)
    public R appInfo(IamApp entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        IamApp app = iamAppService.selectById(entity.getId());
        return R.ok(app);
    }

    @PostMapping(Route.APP_CREATE)
    public R appCreate(@RequestBody IamApp entity) {
        paramCheck(entity);
        entity = iamAppService.create(entity);
        return R.ok(entity);
    }

    @PostMapping(Route.APP_UPDATE)
    public R appUpdate(@RequestBody IamApp entity) {
        paramCheck(entity);
        entity = iamAppService.update(entity);
        return R.ok(entity);
    }

    @PostMapping(Route.APP_REMOVE)
    public R appRemove(@RequestBody IamApp entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        entity = iamAppService.remove(entity);
        return R.ok(entity);
    }

    @GetMapping(Route.APP_OPTIONS)
    public R appOptions() {
        List<IamApp> options = iamAppService.getAppOptions();
        return R.ok(options);
    }


    private void paramCheck(IamApp entity) {
        if (entity.getId() == null) {
            // 新增时
        } else {
            Assert.notNull(entity.getVersion(), "version 不能为空");
        }
        Assert.notNull(entity.getAppCode(), "appCode 不能为空");
        Assert.notNull(entity.getAppName(), "appName 不能为空");
        Assert.notNull(entity.getDomain(), "domain 不能为空");
        Assert.notNull(entity.getAuthType(), "authType 不能为空");
    }


}
