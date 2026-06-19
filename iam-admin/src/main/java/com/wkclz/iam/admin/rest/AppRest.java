package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.AppCreateReq;
import com.wkclz.iam.admin.bean.req.AppPageReq;
import com.wkclz.iam.admin.bean.req.AppUpdateReq;
import com.wkclz.iam.admin.bean.resp.AppResp;
import com.wkclz.iam.admin.service.IamAppService;
import com.wkclz.iam.common.entity.IamApp;
import com.wkclz.tool.utils.BeanUtil;
import com.wkclz.web.bean.IdReq;
import com.wkclz.web.bean.RemoveReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping(Route.PREFIX)
@Tag(name = "应用管理", description = "应用管理接口")
public class AppRest {

    @Autowired
    protected IamAppService iamAppService;

    @GetMapping(Route.APP_PAGE)
    @Operation(summary = "应用分页查询")
    public R<PageData<AppResp>> appPage(@Valid AppPageReq req) {
        IamApp entity = BeanUtil.cp(req, IamApp.class);
        PageData<IamApp> page = iamAppService.getAppPage(entity);
        return R.ok(page.convert(AppResp.class));
    }

    @GetMapping(Route.APP_INFO)
    @Operation(summary = "应用详情")
    public R<AppResp> appInfo(@Valid IdReq req) {
        IamApp app = iamAppService.selectById(req.getId());
        return R.ok(BeanUtil.cp(app, AppResp.class));
    }

    @PostMapping(Route.APP_CREATE)
    @Operation(summary = "应用创建")
    public R<AppResp> appCreate(@Valid @RequestBody AppCreateReq req) {
        IamApp entity = BeanUtil.cp(req, IamApp.class);
        entity = iamAppService.create(entity);
        return R.ok(BeanUtil.cp(entity, AppResp.class));
    }

    @PostMapping(Route.APP_UPDATE)
    @Operation(summary = "应用更新")
    public R<AppResp> appUpdate(@Valid @RequestBody AppUpdateReq req) {
        IamApp entity = BeanUtil.cp(req, IamApp.class);
        entity = iamAppService.update(entity);
        return R.ok(BeanUtil.cp(entity, AppResp.class));
    }

    @PostMapping(Route.APP_REMOVE)
    @Operation(summary = "应用删除")
    public R<Void> appRemove(@Valid @RequestBody RemoveReq req) {
        if (req.getId() != null) {
            IamApp entity = new IamApp();
            entity.setId(req.getId());
            iamAppService.remove(entity);
        } else if (req.getIds() != null) {
            for (Long id : req.getIds()) {
                IamApp entity = new IamApp();
                entity.setId(id);
                iamAppService.remove(entity);
            }
        }
        return R.ok();
    }

    @GetMapping(Route.APP_OPTIONS)
    @Operation(summary = "应用选项列表")
    public R<List<AppResp>> appOptions() {
        List<IamApp> options = iamAppService.getAppOptions();
        return R.ok(BeanUtil.cp(options, AppResp.class));
    }

}
