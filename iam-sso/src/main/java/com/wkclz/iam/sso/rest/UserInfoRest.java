package com.wkclz.iam.sso.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.common.dto.IamMenuDto;
import com.wkclz.auth.bean.Principal;
import com.wkclz.auth.context.SecurityContext;
import com.wkclz.iam.sdk.bean.req.ChangePasswordReq;
import com.wkclz.iam.sso.Route;
import com.wkclz.iam.sso.entity.VueRouterMenu;
import com.wkclz.iam.sso.service.IamLoginService;
import com.wkclz.iam.sso.service.SsoResourceService;
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
    private IamLoginService iamLoginService;
    @Autowired
    private SsoResourceService ssoResourceService;

    @GetMapping(Route.USER_INFO)
    @Operation(summary = "获取用户信息")
    public R<Principal> userInfo(HttpServletRequest request) {
        Principal principal = SecurityContext.getPrincipal(request);
        if (principal == null) {
            return R.error("用户未登录");
        }
        return R.ok(principal);
    }

    @GetMapping(Route.USER_MENU_TREE)
    @Operation(summary = "获取用户菜单树")
    public R<List<IamMenuDto>> userMenuTree(HttpServletRequest request) {
        String appCode = SecurityContext.getAppCode(request);
        if (StringUtils.isBlank(appCode)) {
            return R.error("appCode is blank in Headers");
        }
        List<IamMenuDto> tree = ssoResourceService.getUserMenuTree(appCode);
        return R.ok(tree);
    }

    @GetMapping(Route.USER_MENU_TREE_RUOYI)
    @Operation(summary = "获取若依格式菜单树")
    public R<List<VueRouterMenu>> userMenuTreeRuoyi(HttpServletRequest request) {
        String appCode = SecurityContext.getAppCode(request);
        if (StringUtils.isBlank(appCode)) {
            return R.error("appCode is blank in Headers");
        }
        List<IamMenuDto> menus = ssoResourceService.getUserMenuTree(appCode);
        List<VueRouterMenu> vueRouterMenus = ssoResourceService.getUserMenuTreeRuoyi(menus);
        return R.ok(vueRouterMenus);
    }

    @PostMapping(Route.USER_CHANGE_PASSWORD)
    @Operation(summary = "修改密码")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordReq changePasswordReq) {
        iamLoginService.changePassword(changePasswordReq);
        return R.ok();
    }


}
