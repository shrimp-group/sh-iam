package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamAccessKeyApiService;
import com.wkclz.iam.common.entity.IamAccessKeyApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Route.PREFIX)
public class AccessKeyApiRest {

    @Autowired
    protected IamAccessKeyApiService iamAccessKeyApiService;

    @GetMapping(Route.ACCESS_KEY_API_LIST)
    public R accessKeyApiList(IamAccessKeyApi entity) {
        Assert.notNull(entity.getAppId(), "appId 不能为空!");
        List<IamAccessKeyApi> list = iamAccessKeyApiService.getAccessKeyList(entity.getAppId());
        return R.ok(list);
    }

    @PostMapping(Route.ACCESS_KEY_API_BIND)
    public R accessKeyApiBind(@RequestBody IamAccessKeyApi entity) {
        paramCheck(entity);
        entity = iamAccessKeyApiService.create(entity);
        return R.ok(entity);
    }

    @PostMapping(Route.ACCESS_KEY_API_UNBIND)
    public R accessKeyApiUnbind(@RequestBody IamAccessKeyApi entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        entity = iamAccessKeyApiService.remove(entity);
        return R.ok(entity);
    }

    private void paramCheck(IamAccessKeyApi entity) {
        Assert.notNull(entity.getAppCode(), "appCode 不能为空");
        Assert.notNull(entity.getAppId(), "appId 不能为空");
        Assert.notNull(entity.getApiCode(), "apiCode 不能为空");
        if (entity.getId() == null) {
            // 创建操作的参数检查
        } else {
            // 更新操作的参数检查
            Assert.notNull(entity.getVersion(), "version 不能为空");
        }

    }

}
