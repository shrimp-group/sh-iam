package com.wkclz.iam.portal.rest;

import com.wkclz.core.base.R;
import com.wkclz.core.identity.IdentityContext;
import com.wkclz.core.identity.UserIdentity;
import com.wkclz.iam.portal.Route;
import com.wkclz.iam.portal.bean.resp.AppOptionResp;
import com.wkclz.iam.portal.service.PortalAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author shrimp
 */
@RestController
@RequestMapping(Route.PREFIX)
@Validated
@Tag(name = "门户应用", description = "门户应用入口接口")
public class PortalAppRest {

    @Autowired
    private PortalAppService portalAppService;

    @GetMapping(Route.PORTAL_APP_OPTIONS)
    @Operation(summary = "获取可访问应用列表")
    public R<List<AppOptionResp>> appOptions(HttpServletRequest request) {
        UserIdentity identity = IdentityContext.get();
        if (identity == null || StringUtils.isBlank(identity.getUserCode())) {
            return R.error("userCode is blank");
        }
        List<AppOptionResp> resps = portalAppService.listAccessibleApps(identity.getUserCode());
        return R.ok(resps);
    }

}
