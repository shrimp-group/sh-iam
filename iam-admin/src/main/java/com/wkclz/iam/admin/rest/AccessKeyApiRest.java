package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.AccessKeyApiBindReq;
import com.wkclz.iam.admin.bean.resp.AccessKeyApiResp;
import com.wkclz.iam.admin.bean.resp.ApiResp;
import com.wkclz.iam.admin.service.IamAccessKeyApiService;
import com.wkclz.iam.common.entity.IamAccessKeyApi;
import com.wkclz.iam.common.entity.IamApi;
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
@Tag(name = "AK-API管理", description = "访问密钥-API关联管理接口")
public class AccessKeyApiRest {

    @Autowired
    protected IamAccessKeyApiService iamAccessKeyApiService;

    @GetMapping(Route.ACCESS_KEY_API_LIST)
    @Operation(summary = "查询AK关联的API列表")
    public R<List<ApiResp>> accessKeyApiList(@RequestParam @NotBlank(message = "appId 不能为空") String appId) {
        List<IamApi> apis = iamAccessKeyApiService.getAccessKeyList(appId);
        return R.ok(BeanUtil.cp(apis, ApiResp.class));
    }

    @PostMapping(Route.ACCESS_KEY_API_BIND)
    @Operation(summary = "AK-API绑定")
    public R<AccessKeyApiResp> accessKeyApiBind(@Valid @RequestBody AccessKeyApiBindReq req) {
        IamAccessKeyApi entity = BeanUtil.cp(req, IamAccessKeyApi.class);
        entity = iamAccessKeyApiService.create(entity);
        return R.ok(BeanUtil.cp(entity, AccessKeyApiResp.class));
    }

    @PostMapping(Route.ACCESS_KEY_API_UNBIND)
    @Operation(summary = "AK-API解绑")
    public R<Void> accessKeyApiUnbind(@Valid @RequestBody RemoveReq req) {
        IamAccessKeyApi entity = new IamAccessKeyApi();
        entity.setId(req.getId());
        iamAccessKeyApiService.remove(entity);
        return R.ok();
    }

}
