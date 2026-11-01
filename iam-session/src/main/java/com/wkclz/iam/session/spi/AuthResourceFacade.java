package com.wkclz.iam.session.spi;

import com.wkclz.iam.session.bean.auth.AppAuthResource;

import java.util.List;

/**
 * 权限资源供给 SPI — 权限缓存按需加载的数据源契约。
 *
 * <p>实现形态（装配模式参照 {@link ApiRequestControlProvider}：NoOp 始终注册 + 上层实现类
 * 标注 {@code @Primary} 覆盖，不使用 {@code @ConditionalOnMissingBean} —— 其搭配
 * {@code @Component} 在自动配置组件扫描中条件评估不可靠）：</p>
 * <ul>
 *   <li>{@link NoOpAuthResourceFacade} — 兜底（iam-session 自带，返回空资源，权限判断退化为全放行）</li>
 *   <li>LocalAuthResourceFacade — iam-portal 本地实现（@Primary，直查 DB）</li>
 *   <li>RemoteAuthResourceFacade — 远程 HTTP 实现（配置 iam.session.remote.server-url 时装配，AK 验签调用 portal REST）</li>
 * </ul>
 *
 * @author shrimp
 */
public interface AuthResourceFacade {

    /**
     * 按应用一次性加载静态资源（角色/菜单含按钮/角色菜单/菜单API/接口限流/接口字段）。
     *
     * @param appCode 应用编码
     * @return 应用静态资源（不为 null，集合字段不为 null）
     */
    AppAuthResource loadAppResource(String appCode);

    /**
     * 加载用户全部有效角色编码（启用状态且在有效期内，跨应用，计算时与应用角色集求交）。
     *
     * @param userCode 用户编码
     * @return 角色编码列表（不为 null）
     */
    List<String> loadUserRoleCodes(String userCode);
}
