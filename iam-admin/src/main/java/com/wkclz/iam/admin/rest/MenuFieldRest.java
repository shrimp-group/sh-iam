package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.MenuFieldBindReq;
import com.wkclz.iam.admin.bean.req.MenuFieldSaveReq;
import com.wkclz.iam.admin.bean.resp.MenuFieldResp;
import com.wkclz.iam.admin.service.IamMenuFieldService;
import com.wkclz.web.bean.RemoveReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单字段关系管理
 */
@RestController
@RequestMapping(Route.PREFIX)
@Validated
@Tag(name = "菜单字段管理", description = "菜单-字段关联管理接口")
public class MenuFieldRest {

    @Autowired
    private IamMenuFieldService iamMenuFieldService;

    @GetMapping(Route.MENU_FIELD_LIST)
    @Operation(summary = "查询字段组已绑定的字段列表")
    public R<List<MenuFieldResp>> menuFieldList(@RequestParam @NotBlank(message = "menuCode 不能为空") String menuCode) {
        List<MenuFieldResp> list = iamMenuFieldService.listByMenuCode(menuCode);
        return R.ok(list);
    }

    @PostMapping(Route.MENU_FIELD_BIND)
    @Operation(summary = "绑定字段到字段组")
    public R<Void> menuFieldBind(@Valid @RequestBody MenuFieldBindReq req) {
        iamMenuFieldService.bind(req);
        return R.ok();
    }

    @PostMapping(Route.MENU_FIELD_SAVE)
    @Operation(summary = "批量保存字段组的字段绑定")
    public R<Void> menuFieldSave(@Valid @RequestBody MenuFieldSaveReq req) {
        iamMenuFieldService.save(req.getMenuCode(), req.getAppCode(), req.getFieldCodes());
        return R.ok();
    }

    @PostMapping(Route.MENU_FIELD_UNBIND)
    @Operation(summary = "解绑字段")
    public R<Void> menuFieldUnbind(@Valid @RequestBody RemoveReq req) {
        iamMenuFieldService.unbind(req.getId());
        return R.ok();
    }

}
