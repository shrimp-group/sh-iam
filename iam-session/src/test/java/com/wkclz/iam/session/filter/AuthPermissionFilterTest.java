package com.wkclz.iam.session.filter;

import com.wkclz.core.identity.IdentityContext;
import com.wkclz.core.identity.UserIdentity;
import com.wkclz.iam.session.config.IamSessionConfig;
import com.wkclz.iam.session.service.AuthPermissionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthPermissionFilter 单元测试 — 白名单/身份/部署形态（独立=配置 app-code / 合并=全局打平）/鉴权类型/纳管/权限 全分支。
 */
class AuthPermissionFilterTest {

    private static final String URI = "/iam-admin/user/page";
    private static final String APP = "appA";
    private static final String USER = "user_1";

    private IamSessionConfig config;
    private WhiteListMatcher whiteListMatcher;
    private AuthPermissionService authPermissionService;
    private AuthPermissionFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        config = new IamSessionConfig();
        whiteListMatcher = mock(WhiteListMatcher.class);
        authPermissionService = mock(AuthPermissionService.class);
        // 过滤器为 @Resource 字段注入，测试经反射装配（对齐主代码注入风格）
        filter = new AuthPermissionFilter();
        ReflectionTestUtils.setField(filter, "config", config);
        ReflectionTestUtils.setField(filter, "whiteListMatcher", whiteListMatcher);
        ReflectionTestUtils.setField(filter, "authPermissionService", authPermissionService);
        request = new MockHttpServletRequest("GET", URI);
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
        // 默认已认证身份 + appCode
        UserIdentity identity = new UserIdentity();
        identity.setUserCode(USER);
        IdentityContext.set(identity, "token");
        IdentityContext.setAppCode(APP);
    }

    @AfterEach
    void tearDown() {
        IdentityContext.clear();
    }

    private void stubManaged(boolean managed) {
        when(authPermissionService.isManagedApi(anyString(), anyString(), anyString())).thenReturn(managed);
    }

    @Test
    void passesThroughWhenWhiteListed() throws ServletException, IOException {
        when(whiteListMatcher.isWhiteListed(URI)).thenReturn(true);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(authPermissionService, never()).isManagedApi(anyString(), anyString(), anyString());
    }

    @Test
    void passesThroughWhenDbWhiteListed() throws ServletException, IOException {
        // DB 白名单（iam_api.write_flag=1）：本地白名单未命中、isApiWhiteListed 命中 → 放行，不进入纳管/权限校验
        when(whiteListMatcher.isWhiteListed(URI)).thenReturn(false);
        when(authPermissionService.isApiWhiteListed("__GLOBAL__", "GET", URI)).thenReturn(true);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(authPermissionService).isApiWhiteListed("__GLOBAL__", "GET", URI);
        verify(authPermissionService, never()).isManagedApi(anyString(), anyString(), anyString());
        verify(authPermissionService, never()).hasApiPermission(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void returns403WhenNotDbWhiteListed() throws ServletException, IOException {
        // DB 白名单未命中 → 维持原纳管 403 流程
        when(whiteListMatcher.isWhiteListed(URI)).thenReturn(false);
        when(authPermissionService.isApiWhiteListed("__GLOBAL__", "GET", URI)).thenReturn(false);
        when(authPermissionService.requireApiPermission("__GLOBAL__")).thenReturn(true);
        stubManaged(false);
        filter.doFilter(request, response, chain);
        verify(chain, never()).doFilter(request, response);
        assertEquals(403, response.getStatus());
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertEquals("{\"message\":\"无接口访问权限\"}", body);
    }

    @Test
    void passesThroughWhenNoIdentity() throws ServletException, IOException {
        IdentityContext.clear();
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(authPermissionService, never()).isManagedApi(anyString(), anyString(), anyString());
    }

    @Test
    void standaloneDeploymentUsesConfiguredAppCode() throws ServletException, IOException {
        // 独立部署：按配置 app-code 判定，header appCode 缺失不再拦截（鉴权基准脱离调用方）
        config.setAppCode(APP);
        IdentityContext.setAppCode(null);
        when(authPermissionService.requireApiPermission(APP)).thenReturn(false);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(authPermissionService).requireApiPermission(APP);
        verify(authPermissionService, never()).isManagedApi(anyString(), anyString(), anyString());
    }

    @Test
    void mergedDeploymentUsesGlobalKey() throws ServletException, IOException {
        // 合并部署：未配置 app-code → 全局打平键判定，与 header appCode 无关
        when(authPermissionService.requireApiPermission("__GLOBAL__")).thenReturn(false);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(authPermissionService).requireApiPermission("__GLOBAL__");
        verify(authPermissionService, never()).isManagedApi(anyString(), anyString(), anyString());
    }

    @Test
    void returns403WhenApiNotManaged() throws ServletException, IOException {
        // 全量纳管：未纳管接口直接拒绝（对外文案统一为「无接口访问权限」）
        when(authPermissionService.requireApiPermission("__GLOBAL__")).thenReturn(true);
        stubManaged(false);
        filter.doFilter(request, response, chain);
        verify(chain, never()).doFilter(request, response);
        assertEquals(403, response.getStatus());
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertEquals("{\"message\":\"无接口访问权限\"}", body);
        verify(authPermissionService, never()).hasApiPermission(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void passesThroughWhenHasPermission() throws ServletException, IOException {
        when(authPermissionService.requireApiPermission("__GLOBAL__")).thenReturn(true);
        stubManaged(true);
        when(authPermissionService.hasApiPermission("__GLOBAL__", USER, "GET", URI)).thenReturn(true);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void returns403WhenPermissionDenied() throws ServletException, IOException {
        when(authPermissionService.requireApiPermission("__GLOBAL__")).thenReturn(true);
        stubManaged(true);
        when(authPermissionService.hasApiPermission("__GLOBAL__", USER, "GET", URI)).thenReturn(false);
        filter.doFilter(request, response, chain);
        verify(chain, never()).doFilter(request, response);
        assertEquals(403, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertEquals("{\"message\":\"无接口访问权限\"}", body);
    }

    @Test
    void standaloneTokenApiChecksPermissionWithConfiguredKey() throws ServletException, IOException {
        // 独立部署 TOKEN_API：权限判断按配置的 app-code（而非 header appCode）
        config.setAppCode(APP);
        when(authPermissionService.requireApiPermission(APP)).thenReturn(true);
        stubManaged(true);
        when(authPermissionService.hasApiPermission(eq(APP), eq(USER), anyString(), anyString())).thenReturn(true);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(authPermissionService).hasApiPermission(eq(APP), eq(USER), anyString(), anyString());
    }
}
