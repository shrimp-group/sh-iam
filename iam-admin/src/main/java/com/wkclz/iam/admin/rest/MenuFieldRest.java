package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.service.IamMenuFieldService;
import com.wkclz.iam.admin.bean.req.MenuFieldBindReq;
import com.wkclz.iam.admin.bean.req.MenuFieldSaveReq;
import com.wkclz.iam.admin.bean.resp.MenuFieldResp;
import com.wkclz.iam.common.entity.IamMenuField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单字段关系管理
 */
@RestController
@RequestMapping(Route.PREFIX)
public class MenuFieldRest {

    @Autowired
    private IamMenuFieldService iamMenuFieldService;

    /**
     * 查询字段组已绑定的字段列表
     */
    @GetMapping(Route.MENU_FIELD_LIST)
    public R<List<MenuFieldResp>> menuFieldList(String menuCode) {
        Assert.notNull(menuCode, "menuCode 不能为空");
        List<MenuFieldResp> list = iamMenuFieldService.listByMenuCode(menuCode);
        return R.ok(list);
    }

    /**
     * 绑定字段到字段组
     */
    @PostMapping(Route.MENU_FIELD_BIND)
    public R<Void> menuFieldBind(@RequestBody MenuFieldBindReq req) {
        Assert.notNull(req.getAppCode(), "appCode 不能为空");
        Assert.notNull(req.getMenuCode(), "menuCode 不能为空");
        Assert.notNull(req.getFieldCode(), "fieldCode 不能为空");
        iamMenuFieldService.bind(req);
        return R.ok();
    }

    /**
     * 批量保存字段组的字段绑定
     */
    @PostMapping(Route.MENU_FIELD_SAVE)
    public R<Void> menuFieldSave(@RequestBody MenuFieldSaveReq req) {
        Assert.notNull(req.getAppCode(), "appCode 不能为空");
        Assert.notNull(req.getMenuCode(), "menuCode 不能为空");
        iamMenuFieldService.save(req.getMenuCode(), req.getAppCode(), req.getFieldCodes());
        return R.ok();
    }

    /**
     * 解绑字段
     */
    @PostMapping(Route.MENU_FIELD_UNBIND)
    public R<Void> menuFieldUnbind(@RequestBody IamMenuField entity) {
        Assert.notNull(entity.getId(), "id 不能为空");
        iamMenuFieldService.unbind(entity.getId());
        return R.ok();
    }

}
