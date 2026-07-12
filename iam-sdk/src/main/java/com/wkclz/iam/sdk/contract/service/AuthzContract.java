package com.wkclz.iam.sdk.contract.service;

import com.wkclz.iam.sdk.contract.bean.App;
import com.wkclz.iam.sdk.contract.bean.DataDimension;
import com.wkclz.iam.sdk.contract.bean.FieldPermission;
import com.wkclz.auth.bean.MenuNode;
import com.wkclz.auth.bean.Principal;
import com.wkclz.iam.sdk.contract.bean.Tenant;
import com.wkclz.auth.context.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 鉴权契约
 * 覆盖租户/应用/菜单/接口/字段/数据六个维度的鉴权查询
 * <p>
 * 重载规则：
 * - 完整参数版本：显式传入 Principal + 其他参数，用于单元测试和非 HTTP 场景
 * - 上下文重载版本：从 PrincipalContext 自动获取 Principal 等信息，用于业务代码
 * - 请求全自动版本：从 HttpServletRequest 自动获取所有信息，用于过滤器
 *
 * @author shrimp
 */
public interface AuthzContract {

    // ── 1. 租户 ──

    /**
     * 查询用户可访问的租户列表（完整参数）
     *
     * @param principal 用户主体
     * @return 租户列表
     */
    List<Tenant> listTenants(Principal principal);

    /**
     * 查询用户可访问的租户列表（上下文重载）
     */
    default List<Tenant> listTenants() {
        return listTenants(SecurityContext.getPrincipal());
    }

    // ── 2. 应用 ──

    /**
     * 查询用户在指定租户下可访问的应用列表（完整参数）
     *
     * @param principal  用户主体
     * @param tenantCode 租户编码
     * @return 应用列表
     */
    List<App> listApps(Principal principal, String tenantCode);

    /**
     * 查询用户可访问的应用列表（上下文重载：tenantCode 从请求头获取）
     */
    default List<App> listApps() {
        return listApps(SecurityContext.getPrincipal(), SecurityContext.getTenantCode());
    }

    /**
     * 查询用户可访问的应用列表（半上下文重载：Principal 从上下文，tenantCode 显式传入）
     */
    default List<App> listApps(String tenantCode) {
        return listApps(SecurityContext.getPrincipal(), tenantCode);
    }

    // ── 3. 菜单树 ──

    /**
     * 查询用户在指定应用下的菜单树（完整参数）
     *
     * @param principal 用户主体
     * @param appCode   应用编码
     * @return 树根节点列表（多根表示多个顶级菜单）
     */
    List<MenuNode> getMenuTree(Principal principal, String appCode);

    /**
     * 查询用户菜单树（上下文重载：Principal + appCode 均从上下文获取）
     */
    default List<MenuNode> getMenuTree() {
        return getMenuTree(SecurityContext.getPrincipal(), SecurityContext.getAppCode());
    }

    /**
     * 查询用户菜单树（半上下文重载：Principal 从上下文，appCode 显式传入）
     */
    default List<MenuNode> getMenuTree(String appCode) {
        return getMenuTree(SecurityContext.getPrincipal(), appCode);
    }

    // ── 4. 接口鉴权 ──

    /**
     * 判断用户是否有权访问指定 API（完整参数）
     *
     * @param principal 用户主体
     * @param appCode   应用编码
     * @param apiUri    API URI（如 /iam-admin/user/page）
     * @param apiMethod HTTP 方法（GET/POST/PUT/DELETE）
     * @return true=允许；false=拒绝
     */
    boolean canAccessApi(Principal principal, String appCode, String apiUri, String apiMethod);

    /**
     * 接口鉴权（上下文重载：Principal + appCode 从上下文获取，apiUri/apiMethod 显式传入）
     */
    default boolean canAccessApi(String apiUri, String apiMethod) {
        return canAccessApi(SecurityContext.getPrincipal(), SecurityContext.getAppCode(), apiUri, apiMethod);
    }

    /**
     * 接口鉴权（从当前请求自动获取 apiUri/apiMethod，过滤器场景）
     */
    default boolean canAccessApi(HttpServletRequest request) {
        return canAccessApi(SecurityContext.getPrincipal(), SecurityContext.getAppCode(),
            request.getRequestURI(), request.getMethod());
    }

    // ── 5. 字段权限 ──

    /**
     * 查询用户在指定菜单下各字段的权限（完整参数）
     *
     * @param principal 用户主体
     * @param appCode   应用编码
     * @param menuCode  菜单编码
     * @return 字段权限列表（包含可见/可编辑标记）
     */
    List<FieldPermission> listFieldPermissions(Principal principal, String appCode, String menuCode);

    /**
     * 查询字段权限（上下文重载）
     */
    default List<FieldPermission> listFieldPermissions(String menuCode) {
        return listFieldPermissions(SecurityContext.getPrincipal(), SecurityContext.getAppCode(), menuCode);
    }

    /**
     * 字段过滤：根据权限过滤字段列表，返回有权限的字段（完整参数）
     *
     * @param principal 用户主体
     * @param appCode   应用编码
     * @param menuCode  菜单编码
     * @param fields    待过滤的字段列表
     * @return 有权限的字段列表
     */
    List<String> filterFields(Principal principal, String appCode, String menuCode, List<String> fields);

    /**
     * 字段过滤（上下文重载）
     */
    default List<String> filterFields(String menuCode, List<String> fields) {
        return filterFields(SecurityContext.getPrincipal(), SecurityContext.getAppCode(), menuCode, fields);
    }

    // ── 6. 数据权限 ──

    /**
     * 查询用户在指定应用下的数据权限维度及授权值（完整参数）
     *
     * @param principal 用户主体
     * @param appCode   应用编码
     * @return 数据维度列表（如"部门"维度 + 授权的部门 ID 列表）
     */
    List<DataDimension> getDataDimensions(Principal principal, String appCode);

    /**
     * 查询数据权限维度（上下文重载）
     */
    default List<DataDimension> getDataDimensions() {
        return getDataDimensions(SecurityContext.getPrincipal(), SecurityContext.getAppCode());
    }

    /**
     * 查询数据权限维度（半上下文重载）
     */
    default List<DataDimension> getDataDimensions(String appCode) {
        return getDataDimensions(SecurityContext.getPrincipal(), appCode);
    }
}
