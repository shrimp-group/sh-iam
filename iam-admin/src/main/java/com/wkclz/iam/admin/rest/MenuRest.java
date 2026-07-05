package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.MenuCreateReq;
import com.wkclz.iam.admin.bean.req.MenuListReq;
import com.wkclz.iam.admin.bean.req.MenuUpdateReq;
import com.wkclz.iam.admin.bean.resp.MenuDetailResp;
import com.wkclz.iam.admin.bean.resp.MenuResp;
import com.wkclz.iam.admin.bean.resp.MenuRoleResp;
import com.wkclz.iam.admin.bean.resp.MenuUserResp;
import com.wkclz.iam.admin.service.IamMenuService;
import com.wkclz.iam.admin.service.IamUserMenuService;
import com.wkclz.iam.common.dto.IamMenuDto;
import com.wkclz.iam.common.entity.IamMenu;
import com.wkclz.tool.utils.BeanUtil;
import com.wkclz.web.bean.IdReq;
import com.wkclz.web.bean.RemoveReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping(Route.PREFIX)
@Tag(name = "菜单管理", description = "菜单管理接口")
public class MenuRest {

    @Autowired
    protected IamMenuService iamMenuService;

    @Autowired
    private IamUserMenuService iamUserMenuService;

    @GetMapping(Route.MENU_LIST)
    @Operation(summary = "菜单列表")
    public R<List<MenuResp>> menuList(@Valid MenuListReq req) {
        IamMenu entity = BeanUtil.cp(req, IamMenu.class);
        List<IamMenuDto> list = iamMenuService.menuList(entity);
        return R.ok(BeanUtil.cp(list, MenuResp.class));
    }

    @GetMapping(Route.MENU_TREE)
    @Operation(summary = "菜单树")
    public R<List<MenuResp>> menuTree(@Valid MenuListReq req) {
        IamMenu entity = BeanUtil.cp(req, IamMenu.class);
        List<IamMenuDto> tree = iamMenuService.menuTree(entity);
        return R.ok(BeanUtil.cp(tree, MenuResp.class));
    }

    @GetMapping(Route.MENU_INFO)
    @Operation(summary = "菜单详情")
    public R<MenuResp> menuInfo(@Valid IdReq req) {
        IamMenu menu = iamMenuService.selectById(req.getId());
        return R.ok(BeanUtil.cp(menu, MenuResp.class));
    }

    @PostMapping(Route.MENU_CREATE)
    @Operation(summary = "菜单创建")
    public R<MenuResp> menuCreate(@Valid @RequestBody MenuCreateReq req) {
        IamMenu entity = BeanUtil.cp(req, IamMenu.class);
        entity = iamMenuService.create(entity);
        return R.ok(BeanUtil.cp(entity, MenuResp.class));
    }

    @PostMapping(Route.MENU_UPDATE)
    @Operation(summary = "菜单更新")
    public R<MenuResp> menuUpdate(@Valid @RequestBody MenuUpdateReq req) {
        IamMenu entity = BeanUtil.cp(req, IamMenu.class);
        entity = iamMenuService.update(entity);
        return R.ok(BeanUtil.cp(entity, MenuResp.class));
    }

    @PostMapping(Route.MENU_REMOVE)
    @Operation(summary = "菜单删除")
    public R<Void> menuRemove(@Valid @RequestBody RemoveReq req) {
        if (req.getId() != null) {
            IamMenu entity = new IamMenu();
            entity.setId(req.getId());
            iamMenuService.remove(entity);
        } else if (req.getIds() != null) {
            for (Long id : req.getIds()) {
                IamMenu entity = new IamMenu();
                entity.setId(id);
                iamMenuService.remove(entity);
            }
        }
        return R.ok();
    }

    @GetMapping(Route.MENU_DETAIL)
    @Operation(summary = "菜单详情页")
    public R<MenuDetailResp> menuDetail(@Valid IdReq req) {
        MenuDetailResp detail = iamMenuService.getMenuDetail(req.getId());
        return R.ok(detail);
    }

    @GetMapping(Route.MENU_BOUND_ROLES)
    @Operation(summary = "菜单绑定角色列表")
    public R<List<MenuRoleResp>> menuBoundRoles(@NotBlank(message = "menuCode不能为空") @RequestParam String menuCode) {
        List<MenuRoleResp> list = iamUserMenuService.getMenuBoundRoles(menuCode);
        return R.ok(list);
    }

    @GetMapping(Route.MENU_BOUND_USERS)
    @Operation(summary = "菜单绑定用户列表")
    public R<List<MenuUserResp>> menuBoundUsers(@NotBlank(message = "menuCode不能为空") @RequestParam String menuCode) {
        List<MenuUserResp> list = iamUserMenuService.getMenuBoundUsers(menuCode);
        return R.ok(list);
    }

}
