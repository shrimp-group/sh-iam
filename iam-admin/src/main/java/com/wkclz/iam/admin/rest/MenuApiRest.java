package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.MenuApiBindReq;
import com.wkclz.iam.admin.bean.resp.ApiBoundResp;
import com.wkclz.iam.admin.bean.resp.MenuApiResp;
import com.wkclz.iam.admin.service.IamMenuApiService;
import com.wkclz.iam.common.entity.IamMenuApi;
import com.wkclz.tool.utils.BeanUtil;
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
@Tag(name = "菜单API管理", description = "菜单-API关联管理接口")
public class MenuApiRest {

    @Autowired
    protected IamMenuApiService iamMenuApiService;

    @GetMapping(Route.MENU_API_LIST)
    @Operation(summary = "查询菜单关联的API列表")
    public R<List<MenuApiResp>> menuApiList(@RequestParam @NotBlank(message = "menuCode 不能为空") String menuCode) {
        List<IamMenuApi> list = iamMenuApiService.getMenuList(menuCode);
        return R.ok(BeanUtil.cp(list, MenuApiResp.class));
    }

    @GetMapping(Route.MENU_API_BOUND_LIST)
    @Operation(summary = "查询菜单已绑定的API详情列表")
    public R<List<ApiBoundResp>> menuApiBoundList(@RequestParam @NotBlank(message = "menuCode 不能为空") String menuCode) {
        List<ApiBoundResp> list = iamMenuApiService.getBoundApis(menuCode);
        return R.ok(list);
    }

    @PostMapping(Route.MENU_API_BIND)
    @Operation(summary = "菜单-API绑定")
    public R<MenuApiResp> menuApiBind(@Valid @RequestBody MenuApiBindReq req) {
        IamMenuApi entity = BeanUtil.cp(req, IamMenuApi.class);
        entity = iamMenuApiService.create(entity);
        return R.ok(BeanUtil.cp(entity, MenuApiResp.class));
    }

    @PostMapping(Route.MENU_API_UNBIND)
    @Operation(summary = "菜单-API解绑")
    public R<Void> menuApiUnbind(@Valid @RequestBody RemoveReq req) {
        IamMenuApi entity = new IamMenuApi();
        entity.setId(req.getId());
        iamMenuApiService.remove(entity);
        return R.ok();
    }

}
