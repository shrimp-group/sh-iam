package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.UserMenuListReq;
import com.wkclz.iam.admin.bean.resp.MenuResp;
import com.wkclz.iam.admin.service.IamUserMenuService;
import com.wkclz.iam.common.dto.IamMenuDto;
import com.wkclz.iam.common.entity.IamMenu;
import com.wkclz.auth.context.SecurityContext;
import com.wkclz.tool.utils.BeanUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping(Route.PREFIX)
@Tag(name = "用户菜单", description = "用户菜单查询接口")
public class UserMenuRest {

    @Autowired
    private IamUserMenuService iamUserMenuService;

    @GetMapping(Route.USER_MENU_LIST)
    @Operation(summary = "查询用户菜单列表")
    public R<List<MenuResp>> userMenuList(@Valid UserMenuListReq req) {
        IamMenu entity = BeanUtil.cp(req, IamMenu.class);
        entity.setUserCode(SecurityContext.getUserCode());
        List<IamMenuDto> menus = iamUserMenuService.userMenuList(entity);
        return R.ok(BeanUtil.cp(menus, MenuResp.class));
    }

    @GetMapping(Route.USER_MENU_TREE)
    @Operation(summary = "查询用户菜单树")
    public R<List<MenuResp>> userMenuTree(@Valid UserMenuListReq req) {
        IamMenu entity = BeanUtil.cp(req, IamMenu.class);
        entity.setUserCode(SecurityContext.getUserCode());
        List<IamMenuDto> menus = iamUserMenuService.userMenuTree(entity);
        return R.ok(BeanUtil.cp(menus, MenuResp.class));
    }

}
