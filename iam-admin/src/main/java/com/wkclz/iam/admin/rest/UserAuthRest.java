package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.UserAuthCreateReq;
import com.wkclz.iam.admin.bean.req.UserAuthListReq;
import com.wkclz.iam.admin.bean.req.UserAuthResetPasswordReq;
import com.wkclz.iam.admin.bean.req.UserAuthUpdateReq;
import com.wkclz.iam.admin.bean.resp.UserAuthResp;
import com.wkclz.iam.admin.service.IamUserAuthPasswordService;
import com.wkclz.iam.admin.service.IamUserAuthService;
import com.wkclz.iam.common.entity.IamUserAuth;
import com.wkclz.tool.utils.BeanUtil;
import com.wkclz.web.bean.RemoveReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping(Route.PREFIX)
@Tag(name = "用户认证管理", description = "用户认证方式管理接口")
public class UserAuthRest {

    @Autowired
    protected IamUserAuthService iamUserAuthService;
    @Autowired
    private IamUserAuthPasswordService iamUserAuthPasswordService;

    @GetMapping(Route.USER_AUTH_LIST)
    @Operation(summary = "用户认证列表查询")
    public R<List<UserAuthResp>> userAuthList(@Valid UserAuthListReq req) {
        IamUserAuth entity = BeanUtil.cp(req, IamUserAuth.class);
        List<IamUserAuth> list = iamUserAuthService.selectByEntity(entity);
        return R.ok(BeanUtil.cp(list, UserAuthResp.class));
    }

    @GetMapping(Route.USER_AUTH_INFO)
    @Operation(summary = "用户认证详情")
    public R<UserAuthResp> userAuthInfo(@NotNull(message = "id不能为空") @RequestParam Long id) {
        IamUserAuth result = iamUserAuthService.selectById(id);
        return R.ok(BeanUtil.cp(result, UserAuthResp.class));
    }

    @PostMapping(Route.USER_AUTH_CREATE)
    @Operation(summary = "用户认证创建")
    public R<UserAuthResp> userAuthCreate(@Valid @RequestBody UserAuthCreateReq req) {
        IamUserAuth entity = BeanUtil.cp(req, IamUserAuth.class);
        entity = iamUserAuthService.create(entity);
        return R.ok(BeanUtil.cp(entity, UserAuthResp.class));
    }

    @PostMapping(Route.USER_AUTH_UPDATE)
    @Operation(summary = "用户认证更新")
    public R<UserAuthResp> userAuthUpdate(@Valid @RequestBody UserAuthUpdateReq req) {
        IamUserAuth entity = BeanUtil.cp(req, IamUserAuth.class);
        entity = iamUserAuthService.update(entity);
        return R.ok(BeanUtil.cp(entity, UserAuthResp.class));
    }

    @PostMapping(Route.USER_AUTH_REMOVE)
    @Operation(summary = "用户认证删除")
    public R<Void> userAuthRemove(@Valid @RequestBody RemoveReq req) {
        if (req.getId() != null) {
            IamUserAuth entity = new IamUserAuth();
            entity.setId(req.getId());
            iamUserAuthService.remove(entity);
        } else if (req.getIds() != null) {
            for (Long id : req.getIds()) {
                IamUserAuth entity = new IamUserAuth();
                entity.setId(id);
                iamUserAuthService.remove(entity);
            }
        }
        return R.ok();
    }

    @PostMapping(Route.USER_AUTH_RESET_PASSWORD)
    @Operation(summary = "重置用户密码")
    public R<Void> userAuthResetPassword(@Valid @RequestBody UserAuthResetPasswordReq req) {
        iamUserAuthPasswordService.resetPassword(req.getUserCode(), req.getPassword());
        return R.ok();
    }

}
