package com.wkclz.auth.context;

import com.wkclz.auth.bean.Principal;
import com.wkclz.auth.config.AuthConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.AntPathMatcher;

/**
 * 请求级安全上下文（ThreadLocal + HttpServletRequest 双存储）
 */
public class SecurityContext {

    private static final ThreadLocal<Principal> PRINCIPAL_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> TOKEN_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_CODE_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> APP_CODE_HOLDER = new ThreadLocal<>();

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    // === ThreadLocal get/set ===

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

    // === 从 HttpServletRequest 读取（兼容 PrincipalContext 语义）===

    /**
     * 从请求获取 Principal。优先 ThreadLocal，其次 request attribute。
     */
    public static Principal getPrincipal(HttpServletRequest request) {
        Principal p = PRINCIPAL_HOLDER.get();
        if (p != null) return p;
        return (Principal) request.getAttribute("PRINCIPAL");
    }

    /**
     * 从请求获取 Token。优先 ThreadLocal，其次请求头。
     */
    public static String getToken(HttpServletRequest request) {
        String token = TOKEN_HOLDER.get();
        if (token != null) return token;
        String bearer = request.getHeader(AuthConstants.DEFAULT_TOKEN_HEADER);
        if (bearer != null && bearer.startsWith(AuthConstants.BEARER_PREFIX)) {
            return bearer.substring(AuthConstants.BEARER_PREFIX.length());
        }
        return request.getHeader(AuthConstants.CUSTOM_TOKEN_HEADER);
    }

    /**
     * 从请求获取租户编码。优先 ThreadLocal，其次请求头。
     */
    public static String getTenantCode(HttpServletRequest request) {
        String tc = TENANT_CODE_HOLDER.get();
        if (tc != null) return tc;
        return request.getHeader("tenant-code");
    }

    /**
     * 从请求获取应用编码。优先 ThreadLocal，其次请求头。
     */
    public static String getAppCode(HttpServletRequest request) {
        String ac = APP_CODE_HOLDER.get();
        if (ac != null) return ac;
        return request.getHeader("app-code");
    }

    /**
     * Ant 路径匹配
     */
    public static boolean match(String pattern, String requestUri) {
        return PATH_MATCHER.match(pattern, requestUri);
    }

    /** 请求结束清理，防止内存泄漏 */
    public static void clear() {
        PRINCIPAL_HOLDER.remove();
        TOKEN_HOLDER.remove();
        TENANT_CODE_HOLDER.remove();
        APP_CODE_HOLDER.remove();
    }
}
