package com.wkclz.iam.admin.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.iam.admin.Route;
import com.wkclz.iam.admin.bean.req.UserCreateReq;
import com.wkclz.iam.admin.bean.req.UserPageReq;
import com.wkclz.iam.admin.bean.req.UserUpdateReq;
import com.wkclz.iam.admin.bean.resp.UserResp;
import com.wkclz.iam.admin.service.IamUserService;
import com.wkclz.iam.common.dto.IamUserDto;
import com.wkclz.iam.common.entity.IamUser;
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
@Tag(name = "用户管理", description = "用户管理接口")
public class UserRest {

    @Autowired
    protected IamUserService iamUserService;

    @GetMapping(Route.USER_PAGE)
    @Operation(summary = "用户分页查询")
    public R<PageData<UserResp>> userPage(@Valid UserPageReq req) {
        IamUser entity = BeanUtil.cp(req, IamUser.class);
        PageData<IamUser> page = iamUserService.userPage(entity);
        return R.ok(page.convert(UserResp.class));
    }

    @GetMapping(Route.USER_INFO)
    @Operation(summary = "用户详情")
    public R<UserResp> userInfo(@Valid IdReq req) {
        IamUser user = iamUserService.selectById(req.getId());
        return R.ok(BeanUtil.cp(user, UserResp.class));
    }

    @PostMapping(Route.USER_CREATE)
    @Operation(summary = "用户创建")
    public R<UserResp> userCreate(@Valid @RequestBody UserCreateReq req) {
        IamUserDto dto = BeanUtil.cp(req, IamUserDto.class);
        dto = iamUserService.customCreate(dto);
        return R.ok(BeanUtil.cp(dto, UserResp.class));
    }

    @PostMapping(Route.USER_UPDATE)
    @Operation(summary = "用户更新")
    public R<UserResp> userUpdate(@Valid @RequestBody UserUpdateReq req) {
        IamUser entity = BeanUtil.cp(req, IamUser.class);
        entity = iamUserService.update(entity);
        return R.ok(BeanUtil.cp(entity, UserResp.class));
    }

    @PostMapping(Route.USER_REMOVE)
    @Operation(summary = "用户删除")
    public R<Void> userRemove(@Valid @RequestBody RemoveReq req) {
        if (req.getId() != null) {
            IamUser entity = new IamUser();
            entity.setId(req.getId());
            iamUserService.remove(entity);
        } else if (req.getIds() != null) {
            for (Long id : req.getIds()) {
                IamUser entity = new IamUser();
                entity.setId(id);
                iamUserService.remove(entity);
            }
        }
        return R.ok();
    }

}
