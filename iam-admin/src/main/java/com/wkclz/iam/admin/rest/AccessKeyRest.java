package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.AccessKeyCreateReq;
import com.wkclz.iam.admin.bean.req.AccessKeyPageReq;
import com.wkclz.iam.admin.bean.req.AccessKeyUpdateReq;
import com.wkclz.iam.admin.bean.resp.AccessKeyResp;
import com.wkclz.iam.admin.service.IamAccessKeyService;
import com.wkclz.iam.common.entity.IamAccessKey;
import com.wkclz.tool.utils.BeanUtil;
import com.wkclz.web.bean.IdReq;
import com.wkclz.web.bean.RemoveReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping(Route.PREFIX)
@Tag(name = "访问密钥管理", description = "访问密钥管理接口")
public class AccessKeyRest {

    @Autowired
    protected IamAccessKeyService iamAccessKeyService;

    @GetMapping(Route.ACCESS_KEY_PAGE)
    @Operation(summary = "访问密钥分页查询")
    public R<PageData<AccessKeyResp>> accessKeyPage(@Valid AccessKeyPageReq req) {
        IamAccessKey entity = BeanUtil.cp(req, IamAccessKey.class);
        PageData<IamAccessKey> page = iamAccessKeyService.getAccessKeyPage(entity);
        return R.ok(page.convert(AccessKeyResp.class));
    }

    @GetMapping(Route.ACCESS_KEY_INFO)
    @Operation(summary = "访问密钥详情")
    public R<AccessKeyResp> accessKeyInfo(@Valid IdReq req) {
        IamAccessKey result = iamAccessKeyService.selectById(req.getId());
        return R.ok(BeanUtil.cp(result, AccessKeyResp.class));
    }

    @PostMapping(Route.ACCESS_KEY_CREATE)
    @Operation(summary = "访问密钥创建")
    public R<AccessKeyResp> accessKeyCreate(@Valid @RequestBody AccessKeyCreateReq req) {
        IamAccessKey entity = BeanUtil.cp(req, IamAccessKey.class);
        entity = iamAccessKeyService.create(entity);
        return R.ok(BeanUtil.cp(entity, AccessKeyResp.class));
    }

    @PostMapping(Route.ACCESS_KEY_UPDATE)
    @Operation(summary = "访问密钥更新")
    public R<AccessKeyResp> accessKeyUpdate(@Valid @RequestBody AccessKeyUpdateReq req) {
        IamAccessKey entity = BeanUtil.cp(req, IamAccessKey.class);
        entity = iamAccessKeyService.update(entity);
        return R.ok(BeanUtil.cp(entity, AccessKeyResp.class));
    }

    @PostMapping(Route.ACCESS_KEY_REMOVE)
    @Operation(summary = "访问密钥删除")
    public R<Void> accessKeyRemove(@Valid @RequestBody RemoveReq req) {
        if (req.getId() != null) {
            IamAccessKey entity = new IamAccessKey();
            entity.setId(req.getId());
            iamAccessKeyService.remove(entity);
        } else if (req.getIds() != null) {
            for (Long id : req.getIds()) {
                IamAccessKey entity = new IamAccessKey();
                entity.setId(id);
                iamAccessKeyService.remove(entity);
            }
        }
        return R.ok();
    }

}
