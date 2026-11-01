package com.wkclz.iam.admin.service;

import com.wkclz.iam.admin.mapper.IamMenuMapper;
import com.wkclz.iam.admin.mapper.IamRoleMenuMapper;
import com.wkclz.iam.admin.mapper.IamUserRoleMapper;
import com.wkclz.iam.common.entity.IamMenu;
import com.wkclz.iam.common.entity.IamRole;
import com.wkclz.iam.common.entity.IamRoleMenu;
import com.wkclz.iam.common.entity.IamUserRole;
import com.wkclz.iam.common.event.AdminSecurityEvent;
import com.wkclz.iam.session.enums.AuthChangeType;
import com.wkclz.iam.session.event.PermissionChangeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Task 12 权限变更事件发布测试 — iam-admin 变更成功路径发布 PermissionChangeEvent。
 */
@ExtendWith(MockitoExtension.class)
class PermissionChangeEventPublishTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void userRoleUnbindPublishesBothEvents() {
        // 过渡态：unbind 同时发布既有 AdminSecurityEvent 与 userChanged（Task 13 移除旧事件）
        IamUserRoleMapper mapper = org.mockito.Mockito.mock(IamUserRoleMapper.class);
        IamUserRoleService service = new IamUserRoleService();
        ReflectionTestUtils.setField(service, "mapper", mapper);
        ReflectionTestUtils.setField(service, "eventPublisher", eventPublisher);

        IamUserRole entity = new IamUserRole();
        entity.setId(1L);
        entity.setUserCode("user_1");
        entity.setRoleCode("role_1");
        when(mapper.selectById(1L)).thenReturn(entity);

        service.unbind(1L);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(captor.capture());
        List<Object> events = captor.getAllValues();
        assertEquals(2, events.size());
        assertInstanceOf(AdminSecurityEvent.class, events.get(0));
        PermissionChangeEvent changeEvent = assertInstanceOf(PermissionChangeEvent.class, events.get(1));
        assertEquals(AuthChangeType.USER, changeEvent.getType());
        assertEquals("user_1", changeEvent.getTargetKey());
    }

    @Test
    void roleMenuSavePublishesAppChanged() {
        IamRoleMenuMapper mapper = org.mockito.Mockito.mock(IamRoleMenuMapper.class);
        IamRoleService iamRoleService = org.mockito.Mockito.mock(IamRoleService.class);
        IamRoleMenuService service = new IamRoleMenuService();
        ReflectionTestUtils.setField(service, "mapper", mapper);
        ReflectionTestUtils.setField(service, "iamRoleService", iamRoleService);
        ReflectionTestUtils.setField(service, "eventPublisher", eventPublisher);

        IamRole role = new IamRole();
        role.setRoleCode("role_1");
        role.setAppCode("appA");
        when(iamRoleService.selectOneByEntity(any())).thenReturn(role);
        // 已绑定 m1，本次传入 m2 → 新增 m2、移除 m1
        IamRoleMenu bound = new IamRoleMenu();
        bound.setRoleCode("role_1");
        bound.setMenuCode("m1");
        when(mapper.getBoundMenuCodes("role_1")).thenReturn(List.of(bound));

        service.saveRoleMenus("role_1", List.of("m2"));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        PermissionChangeEvent event = assertInstanceOf(PermissionChangeEvent.class, captor.getValue());
        assertEquals(AuthChangeType.APP_STATIC, event.getType());
        assertEquals("appA", event.getTargetKey());
    }

    @Test
    void menuRemovePublishesAppChanged() {
        IamMenuMapper mapper = org.mockito.Mockito.mock(IamMenuMapper.class);
        IamMenuFieldService iamMenuFieldService = org.mockito.Mockito.mock(IamMenuFieldService.class);
        IamMenuService service = new IamMenuService();
        ReflectionTestUtils.setField(service, "mapper", mapper);
        ReflectionTestUtils.setField(service, "iamMenuFieldService", iamMenuFieldService);
        ReflectionTestUtils.setField(service, "eventPublisher", eventPublisher);

        IamMenu menu = new IamMenu();
        menu.setId(1L);
        menu.setMenuCode("m1");
        menu.setMenuType("MENU");
        menu.setAppCode("appA");
        when(mapper.selectById(1L)).thenReturn(menu);
        when(mapper.selectCountByEntity(any())).thenReturn(0L);

        service.remove(menu);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        PermissionChangeEvent event = assertInstanceOf(PermissionChangeEvent.class, captor.getValue());
        assertEquals(AuthChangeType.APP_STATIC, event.getType());
        assertEquals("appA", event.getTargetKey());
    }
}
