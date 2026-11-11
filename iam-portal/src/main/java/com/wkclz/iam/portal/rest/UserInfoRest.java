package com.wkclz.iam.portal.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.core.identity.IdentityContext;
import com.wkclz.core.identity.UserIdentity;
import com.wkclz.iam.portal.Route;
import com.wkclz.iam.portal.bean.req.ChangePasswordReq;
import com.wkclz.iam.portal.bean.req.UserSelectReq;
import com.wkclz.iam.portal.bean.resp.UserSelectResp;
import com.wkclz.iam.portal.entity.VueRouterMenu;
import com.wkclz.iam.portal.service.PasswordLoginService;
import com.wkclz.iam.portal.service.SsoResourceService;
import com.wkclz.iam.portal.service.SsoUserService;
import com.wkclz.iam.session.bean.auth.AuthMenu;
import com.wkclz.iam.session.service.AuthPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author shrimp
 */
@RestController
@RequestMapping(Route.PREFIX)
@Validated
@Tag(name = "用户信息", description = "用户信息与菜单资源接口")
public class UserInfoRest {

    @Autowired
    private SsoUserService ssoUserService;
    @Autowired
    private SsoResourceService ssoResourceService;
    @Autowired
    private PasswordLoginService passwordLoginService;
    @Autowired
    private AuthPermissionService authPermissionService;

    @GetMapping(Route.USER_INFO)
    @Operation(summary = "获取用户信息")
    public R<UserIdentity> userInfo(HttpServletRequest request) {
        UserIdentity userIdentity = IdentityContext.get();
        return R.ok(userIdentity);
    }

    @GetMapping(Route.USER_MENU_TREE)
    @Operation(summary = "获取用户菜单树")
    public R<List<AuthMenu>> userMenuTree(HttpServletRequest request) {
        String appCode = IdentityContext.getAppCode();
        if (StringUtils.isBlank(appCode)) {
            return R.error("appCode is blank in Headers");
        }
        UserIdentity identity = IdentityContext.get();
        if (identity == null || StringUtils.isBlank(identity.getUserCode())) {
            return R.error("userCode is blank");
        }
        return R.ok(authPermissionService.getUserMenuTree(identity.getUserCode(), appCode));
    }

    @GetMapping(Route.USER_MENU_TREE_RUOYI)
    @Operation(summary = "获取若依格式菜单树")
    public R<List<VueRouterMenu>> userMenuTreeRuoyi(HttpServletRequest request) {
        String appCode = IdentityContext.getAppCode();
        if (StringUtils.isBlank(appCode)) {
            return R.error("appCode is blank in Headers");
        }
        UserIdentity identity = IdentityContext.get();
        if (identity == null || StringUtils.isBlank(identity.getUserCode())) {
            return R.error("userCode is blank");
        }
        List<AuthMenu> tree = authPermissionService.getUserMenuTree(identity.getUserCode(), appCode);
        return R.ok(ssoResourceService.getUserMenuTreeRuoyi(tree));
    }

    @PostMapping(Route.USER_CHANGE_PASSWORD)
    @Operation(summary = "修改密码")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordReq changePasswordReq) {
        passwordLoginService.changePassword(changePasswordReq);
        return R.ok();
    }

    @GetMapping(Route.USER_OPTIONS)
    @Operation(summary = "用户选择分页查询", description = "仅启用用户；用户名/姓名模糊、性别精确；按用户名升序")
    public R<PageData<UserSelectResp>> userOptions(@Valid UserSelectReq req) {
        return R.ok(ssoUserService.getUserSelectPage(req));
    }


}
