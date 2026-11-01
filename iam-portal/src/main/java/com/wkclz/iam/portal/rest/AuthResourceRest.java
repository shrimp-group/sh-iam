package com.wkclz.iam.portal.rest;

import com.alibaba.fastjson2.JSON;
import com.wkclz.core.base.R;
import com.wkclz.iam.portal.Route;
import com.wkclz.iam.portal.bean.req.AuthAppResourceReq;
import com.wkclz.iam.portal.bean.req.AuthUserRolesReq;
import com.wkclz.iam.portal.config.IamPortalConfig;
import com.wkclz.iam.portal.service.LocalAuthResourceFacade;
import com.wkclz.iam.session.bean.auth.AppAuthResource;
import com.wkclz.iam.session.bean.auth.PermissionChangeCommand;
import com.wkclz.iam.session.event.PermissionChangePublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 权限资源远程取数端点 — 供 iam-session 远程形态（RemoteAuthResourceFacade）经 HTTP 拉取权限资源与变更指令。
 *
 * <p>路径 /iam-portal/public/auth/** 属会话白名单，鉴权由本端点的简单 token 校验承担
 * （请求头 auth-token 与配置 iam.portal.auth.token 比对；密钥对方式后续重新设计）。</p>
 */
@Slf4j
@RestController
@RequestMapping(Route.PREFIX)
@Validated
@Tag(name = "权限资源", description = "iam-session 远程形态取数接口（token 鉴权）")
public class AuthResourceRest {

    private static final String HEADER_AUTH_TOKEN = "auth-token";

    @Resource
    private LocalAuthResourceFacade localAuthResourceFacade;
    @Resource
    private IamPortalConfig config;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @GetMapping(Route.PUBLIC_AUTH_APP_RESOURCE)
    @Operation(summary = "按应用加载静态资源")
    public R<AppAuthResource> appResource(@Valid AuthAppResourceReq req, HttpServletRequest request,
                                          HttpServletResponse response) throws IOException {
        log.info("远程取数-按应用加载静态资源: appCode={}", req.getAppCode());
        if (!verifyToken(request, response)) {
            return null;
        }
        return R.ok(localAuthResourceFacade.loadAppResource(req.getAppCode()));
    }

    @GetMapping(Route.PUBLIC_AUTH_USER_ROLES)
    @Operation(summary = "按用户加载有效角色")
    public R<List<String>> userRoles(@Valid AuthUserRolesReq req, HttpServletRequest request,
                                     HttpServletResponse response) throws IOException {
        log.info("远程取数-按用户加载有效角色: userCode={}", req.getUserCode());
        if (!verifyToken(request, response)) {
            return null;
        }
        return R.ok(localAuthResourceFacade.loadUserRoleCodes(req.getUserCode()));
    }

    @GetMapping(Route.PUBLIC_AUTH_CHANGES)
    @Operation(summary = "拉取权限变更指令（每键最新）")
    public R<List<PermissionChangeCommand>> changes(HttpServletRequest request,
                                                    HttpServletResponse response) throws IOException {
        log.info("远程取数-拉取权限变更指令");
        if (!verifyToken(request, response)) {
            return null;
        }
        List<PermissionChangeCommand> commands = new ArrayList<>();
        for (Object value : stringRedisTemplate.opsForHash().values(PermissionChangePublisher.CHANGE_LOG_KEY)) {
            try {
                commands.add(JSON.parseObject(value.toString(), PermissionChangeCommand.class));
            } catch (Exception e) {
                log.warn("权限变更指令解析失败，跳过: error={}", e.getMessage());
            }
        }
        return R.ok(commands);
    }

    /**
     * 简单 token 校验：配置与请求头 auth-token 均非空且相等即通过（配置未按 fail-closed 拒绝）。
     * 不通过时写出 401 响应并返回 false。
     */
    private boolean verifyToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = request.getHeader(HEADER_AUTH_TOKEN);
        String expected = config.getAuthToken();
        if (StringUtils.isBlank(expected)) {
            log.warn("token 校验失败: iam.portal.auth.token 未配置, uri={}", request.getRequestURI());
            return writeUnauthorized(response);
        }
        if (StringUtils.isBlank(token) || !token.equals(expected)) {
            log.warn("token 校验失败: token 不匹配, uri={}", request.getRequestURI());
            return writeUnauthorized(response);
        }
        return true;
    }

    private boolean writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(JSON.toJSONString(R.error("token 校验失败")).getBytes(StandardCharsets.UTF_8));
        return false;
    }

}
