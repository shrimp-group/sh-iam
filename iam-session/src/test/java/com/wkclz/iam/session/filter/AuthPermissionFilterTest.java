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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthPermissionFilter 单元测试 — 开关/白名单/身份/appCode/纳管/权限 全分支。
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
        config.setAuthCheckEnabled(true);
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
    void passesThroughWhenCheckDisabled() throws ServletException, IOException {
        config.setAuthCheckEnabled(false);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(authPermissionService, never()).isManagedApi(anyString(), anyString(), anyString());
    }

    @Test
    void passesThroughWhenWhiteListed() throws ServletException, IOException {
        when(whiteListMatcher.isWhiteListed(URI)).thenReturn(true);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(authPermissionService, never()).isManagedApi(anyString(), anyString(), anyString());
    }

    @Test
    void passesThroughWhenNoIdentity() throws ServletException, IOException {
        IdentityContext.clear();
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(authPermissionService, never()).isManagedApi(anyString(), anyString(), anyString());
    }

    @Test
    void returns403WhenNoAppCode() throws ServletException, IOException {
        // appCode 必需：缺失直接拒绝（无法定位应用静态资源）
        IdentityContext.setAppCode(null);
        filter.doFilter(request, response, chain);
        verify(chain, never()).doFilter(request, response);
        assertEquals(403, response.getStatus());
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertEquals("{\"message\":\"缺少应用标识(appCode)\"}", body);
        verify(authPermissionService, never()).isManagedApi(anyString(), anyString(), anyString());
    }

    @Test
    void returns403WhenApiNotManaged() throws ServletException, IOException {
        // 开关开启后全量纳管：未纳管接口直接拒绝
        stubManaged(false);
        filter.doFilter(request, response, chain);
        verify(chain, never()).doFilter(request, response);
        assertEquals(403, response.getStatus());
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertEquals("{\"message\":\"接口未纳管\"}", body);
        verify(authPermissionService, never()).hasApiPermission(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void passesThroughWhenHasPermission() throws ServletException, IOException {
        stubManaged(true);
        when(authPermissionService.hasApiPermission(APP, USER, "GET", URI)).thenReturn(true);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    void returns403WhenPermissionDenied() throws ServletException, IOException {
        stubManaged(true);
        when(authPermissionService.hasApiPermission(APP, USER, "GET", URI)).thenReturn(false);
        filter.doFilter(request, response, chain);
        verify(chain, never()).doFilter(request, response);
        assertEquals(403, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        String body = response.getContentAsString(StandardCharsets.UTF_8);
        assertEquals("{\"message\":\"无接口访问权限\"}", body);
    }
}
