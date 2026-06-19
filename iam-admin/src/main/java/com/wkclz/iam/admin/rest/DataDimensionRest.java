package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.DataDimCreateReq;
import com.wkclz.iam.admin.bean.req.DataDimPageReq;
import com.wkclz.iam.admin.bean.req.DataDimUpdateReq;
import com.wkclz.iam.admin.bean.resp.DataDimResp;
import com.wkclz.iam.admin.service.IamDataDimensionService;
import com.wkclz.iam.common.entity.IamDataDimension;
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
@Tag(name = "数据维度管理", description = "数据权限维度管理接口")
public class DataDimensionRest {

    @Autowired
    protected IamDataDimensionService iamDataDimensionService;

    @GetMapping(Route.DATA_DIM_PAGE)
    @Operation(summary = "数据维度分页查询")
    public R<PageData<DataDimResp>> dataDimPage(@Valid DataDimPageReq req) {
        IamDataDimension entity = BeanUtil.cp(req, IamDataDimension.class);
        PageData<IamDataDimension> page = iamDataDimensionService.getDataDimensionPage(entity);
        return R.ok(page.convert(DataDimResp.class));
    }

    @GetMapping(Route.DATA_DIM_INFO)
    @Operation(summary = "数据维度详情")
    public R<DataDimResp> dataDimInfo(@Valid IdReq req) {
        IamDataDimension result = iamDataDimensionService.selectById(req.getId());
        return R.ok(BeanUtil.cp(result, DataDimResp.class));
    }

    @PostMapping(Route.DATA_DIM_CREATE)
    @Operation(summary = "数据维度创建")
    public R<DataDimResp> dataDimCreate(@Valid @RequestBody DataDimCreateReq req) {
        IamDataDimension entity = BeanUtil.cp(req, IamDataDimension.class);
        entity = iamDataDimensionService.create(entity);
        return R.ok(BeanUtil.cp(entity, DataDimResp.class));
    }

    @PostMapping(Route.DATA_DIM_UPDATE)
    @Operation(summary = "数据维度更新")
    public R<DataDimResp> dataDimUpdate(@Valid @RequestBody DataDimUpdateReq req) {
        IamDataDimension entity = BeanUtil.cp(req, IamDataDimension.class);
        entity = iamDataDimensionService.update(entity);
        return R.ok(BeanUtil.cp(entity, DataDimResp.class));
    }

    @PostMapping(Route.DATA_DIM_REMOVE)
    @Operation(summary = "数据维度删除")
    public R<Void> dataDimRemove(@Valid @RequestBody RemoveReq req) {
        if (req.getId() != null) {
            IamDataDimension entity = new IamDataDimension();
            entity.setId(req.getId());
            iamDataDimensionService.remove(entity);
        } else if (req.getIds() != null) {
            for (Long id : req.getIds()) {
                IamDataDimension entity = new IamDataDimension();
                entity.setId(id);
                iamDataDimensionService.remove(entity);
            }
        }
        return R.ok();
    }

    @GetMapping(Route.DATA_DIM_OPTIONS)
    @Operation(summary = "数据维度选项列表")
    public R<List<DataDimResp>> dataDimOption() {
        List<IamDataDimension> list = iamDataDimensionService.selectAll();
        return R.ok(BeanUtil.cp(list, DataDimResp.class));
    }

}
