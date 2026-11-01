package com.wkclz.iam.portal.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.portal.bean.req.AuthAppResourceReq;
import com.wkclz.iam.portal.bean.req.AuthUserRolesReq;
import com.wkclz.iam.portal.config.IamPortalConfig;
import com.wkclz.iam.portal.service.LocalAuthResourceFacade;
import com.wkclz.iam.session.bean.auth.AppAuthResource;
import com.wkclz.iam.session.bean.auth.PermissionChangeCommand;
import com.wkclz.iam.session.event.PermissionChangePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * AuthResourceRest 单元测试 — token 校验分支（通过/缺头/不匹配/未配置）与数据返回。
 */
class AuthResourceRestTest {

    private static final String TOKEN = "shared-token";

    private LocalAuthResourceFacade localAuthResourceFacade;
    private StringRedisTemplate stringRedisTemplate;
    private AuthResourceRest rest;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        localAuthResourceFacade = mock(LocalAuthResourceFacade.class);
        stringRedisTemplate = mock(StringRedisTemplate.class);
        IamPortalConfig config = new IamPortalConfig();
        config.setAuthToken(TOKEN);
        rest = new AuthResourceRest();
        ReflectionTestUtils.setField(rest, "localAuthResourceFacade", localAuthResourceFacade);
        ReflectionTestUtils.setField(rest, "config", config);
        ReflectionTestUtils.setField(rest, "stringRedisTemplate", stringRedisTemplate);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.addHeader("auth-token", TOKEN);
    }

    @Test
    void appResourceReturnsDataWhenTokenOk() throws Exception {
        AppAuthResource resource = AppAuthResource.empty("appA");
        when(localAuthResourceFacade.loadAppResource("appA")).thenReturn(resource);

        AuthAppResourceReq req = new AuthAppResourceReq();
        req.setAppCode("appA");
        R<AppAuthResource> result = rest.appResource(req, request, response);

        assertEquals(200, result.getCode());
        assertEquals(resource, result.getData());
    }

    @Test
    void returns401WhenTokenHeaderMissing() throws Exception {
        request.removeHeader("auth-token");

        AuthAppResourceReq req = new AuthAppResourceReq();
        req.setAppCode("appA");
        rest.appResource(req, request, response);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString(StandardCharsets.UTF_8).contains("token 校验失败"));
        verifyNoInteractions(localAuthResourceFacade);
    }

    @Test
    void returns401WhenTokenMismatch() throws Exception {
        request.removeHeader("auth-token");
        request.addHeader("auth-token", "wrong-token");

        AuthAppResourceReq req = new AuthAppResourceReq();
        req.setAppCode("appA");
        rest.appResource(req, request, response);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(localAuthResourceFacade);
    }

    @Test
    void returns401WhenTokenNotConfigured() throws Exception {
        ReflectionTestUtils.setField(rest, "config", new IamPortalConfig());

        AuthAppResourceReq req = new AuthAppResourceReq();
        req.setAppCode("appA");
        rest.appResource(req, request, response);

        assertEquals(401, response.getStatus());
        verifyNoInteractions(localAuthResourceFacade);
    }

    @Test
    void userRolesReturnsDataWhenTokenOk() throws Exception {
        when(localAuthResourceFacade.loadUserRoleCodes("user_1")).thenReturn(List.of("role_1"));

        AuthUserRolesReq req = new AuthUserRolesReq();
        req.setUserCode("user_1");
        R<List<String>> result = rest.userRoles(req, request, response);

        assertEquals(200, result.getCode());
        assertEquals(List.of("role_1"), result.getData());
    }

    @Test
    void changesReturnsEmptyListWhenHashEmpty() throws Exception {
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.values(PermissionChangePublisher.CHANGE_LOG_KEY)).thenReturn(List.of());

        R<List<PermissionChangeCommand>> result = rest.changes(request, response);

        assertEquals(200, result.getCode());
        assertTrue(result.getData().isEmpty());
    }
}
