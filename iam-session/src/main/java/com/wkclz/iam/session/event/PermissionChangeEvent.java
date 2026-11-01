package com.wkclz.iam.session.event;

import com.wkclz.iam.session.enums.AuthChangeType;
import lombok.Getter;

/**
 * 权限变更事件 — 不可变领域事件，iam-admin 各变更入口（角色/菜单/API/用户角色/请求控制等）
 * 经 Spring 事件机制发布；由 {@link PermissionChangePublisher} 消费（AFTER_COMMIT 发布至 Redis）。
 *
 * <p>类型语义见 {@link AuthChangeType}：APP_STATIC（targetKey=appCode）／USER（targetKey=userCode）。</p>
 *
 * @author shrimp
 */
@Getter
public class PermissionChangeEvent {

    private final AuthChangeType type;
    private final String targetKey;
    private final long timestamp;

    private PermissionChangeEvent(AuthChangeType type, String targetKey) {
        this.type = type;
        this.targetKey = targetKey;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 应用静态资源变更（角色/菜单/API/绑定关系等）。
     */
    public static PermissionChangeEvent appChanged(String appCode) {
        return new PermissionChangeEvent(AuthChangeType.APP_STATIC, appCode);
    }

    /**
     * 用户级资源变更（用户角色绑定/用户状态变更等）。
     */
    public static PermissionChangeEvent userChanged(String userCode) {
        return new PermissionChangeEvent(AuthChangeType.USER, userCode);
    }

}
