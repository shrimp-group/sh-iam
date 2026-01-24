package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamAccessKeyService;
import com.wkclz.iam.common.entity.IamAccessKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(Route.PREFIX)
public class AccessKeyRest {

    @Autowired
    protected IamAccessKeyService iamAccessKeyService;

    @GetMapping(Route.ACCESS_KEY_PAGE)
    public R accessKeyPage(IamAccessKey entity) {
        PageData<IamAccessKey> page = iamAccessKeyService.getAccessKeyPage(entity);
        return R.ok(page);
    }

    @GetMapping(Route.ACCESS_KEY_INFO)
    public R accessKeyInfo(IamAccessKey entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        IamAccessKey accessKey = iamAccessKeyService.selectById(entity.getId());
        return R.ok(accessKey);
    }

    @PostMapping(Route.ACCESS_KEY_CREATE)
    public R accessKeyCreate(@RequestBody IamAccessKey entity) {
        paramCheck(entity);
        entity = iamAccessKeyService.create(entity);
        return R.ok(entity);
    }

    @PostMapping(Route.ACCESS_KEY_UPDATE)
    public R accessKeyUpdate(@RequestBody IamAccessKey entity) {
        paramCheck(entity);
        entity = iamAccessKeyService.update(entity);
        return R.ok(entity);
    }

    @PostMapping(Route.ACCESS_KEY_REMOVE)
    public R accessKeyRemove(@RequestBody IamAccessKey entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        entity = iamAccessKeyService.remove(entity);
        return R.ok(entity);
    }

    private void paramCheck(IamAccessKey entity) {
        Assert.notNull(entity.getAppCode(), "getAppCode 不能为空");
        if (entity.getId() == null) {
            // 创建操作的参数检查
        } else {
            // 更新操作的参数检查
            Assert.notNull(entity.getVersion(), "version 不能为空");
        }
    }

}
