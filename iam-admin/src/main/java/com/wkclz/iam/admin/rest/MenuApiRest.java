package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamMenuApiService;
import com.wkclz.iam.common.entity.IamMenuApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Route.PREFIX)
public class MenuApiRest {

    @Autowired
    protected IamMenuApiService iamMenuApiService;

    @GetMapping(Route.MENU_API_LIST)
    public R menuApiList(IamMenuApi entity) {
        Assert.notNull(entity.getMenuCode(), "menuCode 不能为空!");
        List<IamMenuApi> list = iamMenuApiService.getMenuList(entity.getMenuCode());
        return R.ok(list);
    }

    @PostMapping(Route.MENU_API_CREATE)
    public R menuApiCreate(@RequestBody IamMenuApi entity) {
        paramCheck(entity);
        entity = iamMenuApiService.create(entity);
        return R.ok(entity);
    }

    @PostMapping(Route.MENU_API_REMOVE)
    public R menuApiRemove(@RequestBody IamMenuApi entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        entity = iamMenuApiService.remove(entity);
        return R.ok(entity);
    }

    private void paramCheck(IamMenuApi entity) {
        Assert.notNull(entity.getAppCode(), "appCode 不能为空");
        Assert.notNull(entity.getMenuCode(), "menuCode 不能为空");
        Assert.notNull(entity.getApiCode(), "apiCode 不能为空");
        if (entity.getId() == null) {
            // 创建操作的参数检查
        } else {
            // 更新操作的参数检查
            Assert.notNull(entity.getVersion(), "version 不能为空");
        }

    }

}
