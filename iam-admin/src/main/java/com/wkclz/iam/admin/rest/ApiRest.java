package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.ApiCreateReq;
import com.wkclz.iam.admin.bean.req.ApiListReq;
import com.wkclz.iam.admin.bean.req.ApiPageReq;
import com.wkclz.iam.admin.bean.req.ApiPasteReq;
import com.wkclz.iam.admin.bean.req.ApiUpdateReq;
import com.wkclz.iam.admin.bean.resp.ApiDetailResp;
import com.wkclz.iam.admin.bean.resp.ApiResp;
import com.wkclz.iam.admin.init.RestfulScan;
import com.wkclz.iam.admin.service.IamApiService;
import com.wkclz.iam.common.dto.IamApiDto;
import com.wkclz.iam.common.entity.IamApi;
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
@Tag(name = "API管理", description = "API管理接口")
public class ApiRest {

    @Autowired
    private RestfulScan restfulScan;
    @Autowired
    protected IamApiService iamApiService;

    @GetMapping(Route.API_PAGE)
    @Operation(summary = "API分页查询")
    public R<PageData<ApiResp>> apiPage(@Valid ApiPageReq req) {
        IamApiDto entity = BeanUtil.cp(req, IamApiDto.class);
        PageData<IamApiDto> page = iamApiService.getApiPage(entity);
        return R.ok(page.convert(ApiResp.class));
    }

    @GetMapping(Route.API_INFO)
    @Operation(summary = "API详情")
    public R<ApiResp> apiInfo(@Valid IdReq req) {
        IamApi api = iamApiService.selectById(req.getId());
        return R.ok(BeanUtil.cp(api, ApiResp.class));
    }

    @GetMapping(Route.API_DETAIL)
    @Operation(summary = "API详情页")
    public R<ApiDetailResp> apiDetail(@Valid IdReq req) {
        ApiDetailResp detail = iamApiService.getApiDetail(req.getId());
        return R.ok(detail);
    }

    @PostMapping(Route.API_CREATE)
    @Operation(summary = "API创建")
    public R<ApiResp> apiCreate(@Valid @RequestBody ApiCreateReq req) {
        IamApi entity = BeanUtil.cp(req, IamApi.class);
        entity = iamApiService.create(entity);
        return R.ok(BeanUtil.cp(entity, ApiResp.class));
    }

    @PostMapping(Route.API_UPDATE)
    @Operation(summary = "API更新")
    public R<ApiResp> apiUpdate(@Valid @RequestBody ApiUpdateReq req) {
        IamApi entity = BeanUtil.cp(req, IamApi.class);
        entity = iamApiService.update(entity);
        return R.ok(BeanUtil.cp(entity, ApiResp.class));
    }

    @PostMapping(Route.API_REMOVE)
    @Operation(summary = "API删除")
    public R<Void> apiRemove(@Valid @RequestBody RemoveReq req) {
        if (req.getId() != null) {
            IamApi entity = new IamApi();
            entity.setId(req.getId());
            iamApiService.remove(entity);
        } else if (req.getIds() != null) {
            for (Long id : req.getIds()) {
                IamApi entity = new IamApi();
                entity.setId(id);
                iamApiService.remove(entity);
            }
        }
        return R.ok();
    }

    @GetMapping(Route.API_OPTIONS)
    @Operation(summary = "API选项列表")
    public R<List<ApiResp>> apiOptions(@Valid ApiListReq req) {
        IamApi entity = BeanUtil.cp(req, IamApi.class);
        List<IamApi> list = iamApiService.getApiOptions(entity);
        return R.ok(BeanUtil.cp(list, ApiResp.class));
    }

    @PostMapping(Route.API_SYNC)
    @Operation(summary = "API同步")
    public R<Void> apiSync() {
        restfulScan.run(null);
        return R.ok();
    }

    @GetMapping(Route.API_COPY)
    @Operation(summary = "API复制")
    public R<List<ApiResp>> apiCopy(@Valid ApiListReq req) {
        IamApi entity = BeanUtil.cp(req, IamApi.class);
        List<IamApi> list = iamApiService.getApis4Copy(entity);
        return R.ok(BeanUtil.cp(list, ApiResp.class));
    }

    @PostMapping(Route.API_PASTE)
    @Operation(summary = "API粘贴")
    public R<Integer> apiPaste(@Valid @RequestBody ApiPasteReq req) {
        Integer count = iamApiService.apiPaste(req.getApis());
        return R.ok(count);
    }

}
