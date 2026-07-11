package com.wkclz.auth.context;

import com.wkclz.auth.bean.Principal;

/**
 * 请求级安全上下文（ThreadLocal）
 */
public class SecurityContext {

    private static final ThreadLocal<Principal> PRINCIPAL_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> TOKEN_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_CODE_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> APP_CODE_HOLDER = new ThreadLocal<>();

    public static Principal getPrincipal() {
        return PRINCIPAL_HOLDER.get();
    }

    public static void setPrincipal(Principal principal) {
        PRINCIPAL_HOLDER.set(principal);
    }

    public static String getToken() {
        return TOKEN_HOLDER.get();
    }

    public static void setToken(String token) {
        TOKEN_HOLDER.set(token);
    }

    public static String getUserCode() {
        Principal p = getPrincipal();
        return p != null ? p.getUserCode() : null;
    }

    public static String getUsername() {
        Principal p = getPrincipal();
        return p != null ? p.getUsername() : null;
    }

    public static String getTenantCode() {
        return TENANT_CODE_HOLDER.get();
    }

    public static void setTenantCode(String tenantCode) {
        TENANT_CODE_HOLDER.set(tenantCode);
    }

    public static String getAppCode() {
        return APP_CODE_HOLDER.get();
    }

    public static void setAppCode(String appCode) {
        APP_CODE_HOLDER.set(appCode);
    }

    /** 请求结束清理，防止内存泄漏 */
    public static void clear() {
        PRINCIPAL_HOLDER.remove();
        TOKEN_HOLDER.remove();
        TENANT_CODE_HOLDER.remove();
        APP_CODE_HOLDER.remove();
    }
}
