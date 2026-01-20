package com.wkclz.iam.admin.rest;

import cn.hutool.core.lang.Assert;
import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamUserMenuService;
import com.wkclz.iam.common.dto.IamMenuDto;
import com.wkclz.iam.common.entity.IamMenu;
import com.wkclz.iam.sdk.helper.SessionHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(Route.PREFIX)
public class UserMenuRest {

    @Autowired
    private IamUserMenuService iamUserMenuService;

    @GetMapping(Route.USER_MENU_LIST)
    public R userMenuList(IamMenu entity) {
        Assert.notNull(entity.getAppCode(), "appCode 不能为空");
        entity.setUserCode(SessionHelper.getUserCode());
        List<IamMenuDto> menus = iamUserMenuService.userMenuList(entity);
        return R.ok(menus);
    }

    @GetMapping(Route.USER_MENU_TREE)
    public R userMenuTree(IamMenu entity) {
        Assert.notNull(entity.getMenuCode(), "appCode 不能为空");
        entity.setUserCode(SessionHelper.getUserCode());
        List<IamMenuDto> menus = iamUserMenuService.userMenuTree(entity);
        return R.ok(menus);
    }

}