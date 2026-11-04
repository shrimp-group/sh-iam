package com.wkclz.iam.session.filter;

import com.wkclz.core.identity.IdentityContext;
import com.wkclz.iam.session.bean.Session;
import com.wkclz.iam.session.bean.TokenInfo;
import com.wkclz.iam.session.config.IamSessionConfig;
import com.wkclz.iam.session.service.AuthPermissionService;
import com.wkclz.iam.session.service.SessionManager;
import com.wkclz.iam.session.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SessionAuthFilter 单元测试 — 本地白名单放行、DB 白名单免登录放行（含 best-effort claims）、
 * 非白名单无会话 401、有效会话放行、独立部署 ownerKey 取配置 app-code。
 */
class SessionAuthFilterTest {

    private static final String URI = "/iam-admin/user/page";
    private static final String TOKEN = "token-1";

    private IamSessionConfig config;
    private WhiteListMatcher whiteListMatcher;
    private AuthPermissionService authPermissionService;
    private SessionManager sessionManager;
    private TokenService tokenService;
    private SessionAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        config = new IamSessionConfig();
        whiteListMatcher = mock(WhiteListMatcher.class);
        authPermissionService = mock(AuthPermissionService.class);
        sessionManager = mock(SessionManager.class);
        tokenService = mock(TokenService.class);
        // 过滤器为 @Autowired 字段注入，测试经反射装配（对齐主代码注入风格）
        filter = new SessionAuthFilter();
        ReflectionTestUtils.setField(filter, "config", config);
        ReflectionTestUtils.setField(filter, "whiteListMatcher", whiteListMatcher);
        ReflectionTestUtils.setField(filter, "authPermissionService", authPermissionService);
        ReflectionTestUtils.setField(filter, "sessionManager", sessionManager);
        ReflectionTestUtils.setField(filter, "tokenService", tokenService);
        request = new MockHttpServletRequest("GET", URI);
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        IdentityContext.clear();
    }

    @Test
    void passesThroughWhenLocalWhiteListed() throws ServletException, IOException {
        when(whiteListMatcher.isWhiteListed(URI)).thenReturn(true);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
        verify(authPermissionService, never()).isApiWhiteListed(anyString(), anyString(), anyString());
    }

    @Test
    void passesThroughWhenDbWhiteListedWithoutSession() throws ServletException, IOException {
        // 合并部署（未配置 app-code）→ ownerKey=__GLOBAL__；DB 白名单命中 → 无有效会话免登录放行
        when(whiteListMatcher.isWhiteListed(URI)).thenReturn(false);
        when(authPermissionService.isApiWhiteListed("__GLOBAL__", "GET", URI)).thenReturn(true);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
        verify(authPermissionService).isApiWhiteListed("__GLOBAL__", "GET", URI);
    }

    @Test
    void dbWhiteListedParsesTokenClaimsBestEffort() throws ServletException, IOException {
        // 携带过期/无效 token 的白名单请求：放行且 best-effort 解析 claims 供审计日志
        request.addHeader("Authorization", "Bearer " + TOKEN);
        when(whiteListMatcher.isWhiteListed(URI)).thenReturn(false);
        when(authPermissionService.isApiWhiteListed("__GLOBAL__", "GET", URI)).thenReturn(true);
        TokenInfo tokenInfo = new TokenInfo();
        tokenInfo.setUserCode("user_1");
        tokenInfo.setUsername("u1");
        when(tokenService.parseClaimsBestEffort(TOKEN)).thenReturn(tokenInfo);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
        assertNotNull(IdentityContext.get());
        assertEquals("user_1", IdentityContext.get().getUserCode());
    }

    @Test
    void returns401WhenNotWhitelistedAndNoSession() throws ServletException, IOException {
        // 非白名单 + 无有效会话 → 401（DB 白名单未命中维持原行为）
        when(whiteListMatcher.isWhiteListed(URI)).thenReturn(false);
        when(authPermissionService.isApiWhiteListed("__GLOBAL__", "GET", URI)).thenReturn(false);
        filter.doFilter(request, response, chain);
        verify(chain, never()).doFilter(request, response);
        assertEquals(401, response.getStatus());
        assertEquals("{\"message\":\"缺少认证 Token\"}", response.getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    void standaloneDeploymentUsesConfiguredAppCode() throws ServletException, IOException {
        // 独立部署：DB 白名单按配置 app-code 判定
        config.setAppCode("appA");
        when(whiteListMatcher.isWhiteListed(URI)).thenReturn(false);
        when(authPermissionService.isApiWhiteListed("appA", "GET", URI)).thenReturn(true);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
        verify(authPermissionService).isApiWhiteListed("appA", "GET", URI);
    }

    @Test
    void passesThroughWithValidSessionWithoutWhitelist() throws ServletException, IOException {
        // 有效会话（JWT + Redis 会话）→ 放行，不再触发 DB 白名单判定
        request.addHeader("Authorization", "Bearer " + TOKEN);
        Session session = new Session();
        session.setUserIdentity("{\"userCode\":\"user_1\",\"username\":\"u1\"}");
        when(sessionManager.validateAndRefresh(TOKEN)).thenReturn(session);
        when(whiteListMatcher.isWhiteListed(URI)).thenReturn(false);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertEquals(200, response.getStatus());
        assertEquals("user_1", IdentityContext.get().getUserCode());
        verify(authPermissionService, never()).isApiWhiteListed(anyString(), anyString(), anyString());
    }

    @Test
    void dbWhiteListCheckSkippedWhenSessionInvalidTokenAbsent() throws ServletException, IOException {
        // 无 token 且 DB 白名单未命中 → 401 且不做 best-effort claims 解析
        when(whiteListMatcher.isWhiteListed(URI)).thenReturn(false);
        when(authPermissionService.isApiWhiteListed("__GLOBAL__", "GET", URI)).thenReturn(false);
        filter.doFilter(request, response, chain);
        verify(tokenService, never()).parseClaimsBestEffort(anyString());
        assertNull(IdentityContext.get());
    }
}
