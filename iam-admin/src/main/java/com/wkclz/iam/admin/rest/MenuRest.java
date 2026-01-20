package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamMenuService;
import com.wkclz.iam.common.dto.IamMenuDto;
import com.wkclz.iam.common.entity.IamMenu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Route.PREFIX)
public class MenuRest {

    @Autowired
    protected IamMenuService iamMenuService;

    @GetMapping(Route.MENU_TREE)
    public R menuTree(IamMenu entity) {
        Assert.notNull(entity.getAppCode(), "appCode 不能为空!");
        List<IamMenuDto> tree = iamMenuService.menuTree(entity);
        return R.ok(tree);
    }

    @GetMapping(Route.MENU_INFO)
    public R menuInfo(IamMenu entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        IamMenu menu = iamMenuService.selectById(entity.getId());
        return R.ok(menu);
    }

    @PostMapping(Route.MENU_CREATE)
    public R menuCreate(@RequestBody IamMenu entity) {
        paramCheck(entity);
        entity = iamMenuService.create(entity);
        return R.ok(entity);
    }

    @PostMapping(Route.MENU_UPDATE)
    public R menuUpdate(@RequestBody IamMenu entity) {
        paramCheck(entity);
        entity = iamMenuService.update(entity);
        return R.ok(entity);
    }

    @PostMapping(Route.MENU_REMOVE)
    public R menuRemove(@RequestBody IamMenu entity) {
        Assert.notNull(entity.getId(), ResultCode.PARAM_NO_ID.getMessage());
        // 检查菜单下是否有子菜单
        IamMenu param = new IamMenu();
        param.setParentCode(entity.getMenuCode());
        long childrenMenuCount = iamMenuService.selectCountByEntity(param);
        if (childrenMenuCount > 0) {
            return R.error("请先删除子菜单");
        }
        entity = iamMenuService.remove(entity);
        return R.ok(entity);
    }

    private void paramCheck(IamMenu entity) {
        if (entity.getId() != null) {
            Assert.notNull(entity.getVersion(), "version 不能为空");
        }
        Assert.notNull(entity.getAppCode(), "appCode 不能为空");
        Assert.notNull(entity.getMenuCode(), "menuCode 不能为空");
        Assert.notNull(entity.getMenuName(), "menuName 不能为空");
        Assert.notNull(entity.getResType(), "resType 不能为空");
    }

}