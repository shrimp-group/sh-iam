package com.wkclz.iam.sdk.contract.context;

import com.wkclz.auth.bean.Principal;
import com.wkclz.iam.sdk.contract.bean.Session;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Principal 读取上下文
 * 替代 sh-core UserContext 的用户信息读取职责
 * <p>
 * 双存储策略：
 * - request.setAttribute: 主存储，跟随请求生命周期，Servlet 规范保证线程安全
 * - ThreadLocal: 辅助存储，支持子线程读取（异步场景），由 clear() 在 finally 中清理
 *
 * @author shrimp
 */
public final class PrincipalContext {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final String ATTR_PRINCIPAL = "contractPrincipal";
    private static final String ATTR_SESSION = "contractSession";

    private static final ThreadLocal<Principal> PRINCIPAL_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Session> SESSION_HOLDER = new ThreadLocal<>();

    private PrincipalContext() {
    }

    // ── 写入（过滤器调用） ──

    /**
     * 缓存 Principal + Session 到当前请求上下文
     *
     * @param request   HTTP 请求
     * @param principal 用户主体
     * @param session   会话信息
     */
    public static void cache(HttpServletRequest request, Principal principal, Session session) {
        if (request != null) {
            request.setAttribute(ATTR_PRINCIPAL, principal);
            request.setAttribute(ATTR_SESSION, session);
        }
        PRINCIPAL_HOLDER.set(principal);
        SESSION_HOLDER.set(session);
    }

    /**
     * 清理上下文（请求结束时调用，防内存泄漏）
     */
    public static void clear() {
        PRINCIPAL_HOLDER.remove();
        SESSION_HOLDER.remove();
    }

    // ── 核心读取 ──

    /**
     * 获取当前 Principal
     *
     * @return Principal；无上下文返回 null
     */
    public static Principal getPrincipal() {
        Principal p = PRINCIPAL_HOLDER.get();
        if (p != null) {
            return p;
        }
        HttpServletRequest request = getRequest();
        if (request != null) {
            return (Principal) request.getAttribute(ATTR_PRINCIPAL);
        }
        return null;
    }

    /**
     * 获取当前 Session
     *
     * @return Session；无上下文返回 null
     */
    public static Session getSession() {
        Session s = SESSION_HOLDER.get();
        if (s != null) {
            return s;
        }
        HttpServletRequest request = getRequest();
        if (request != null) {
            return (Session) request.getAttribute(ATTR_SESSION);
        }
        return null;
    }

    // ── 便捷方法 ──

    /**
     * 获取当前用户编码
     */
    public static String getUserCode() {
        Principal p = getPrincipal();
        return p != null ? p.getUserCode() : null;
    }

    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        Principal p = getPrincipal();
        return p != null ? p.getUsername() : null;
    }

    /**
     * 获取当前昵称
     */
    public static String getNickname() {
        Principal p = getPrincipal();
        return p != null ? p.getNickname() : null;
    }

    /**
     * 获取当前租户编码（动态值，从请求头 tenant-code 获取）
     * 租户可随时切换，不属于用户身份
     *
     * @return 租户编码；无请求上下文返回 null
     */
    public static String getTenantCode() {
        HttpServletRequest request = getRequest();
        if (request != null) {
            String tc = request.getHeader("tenant-code");
            if (StringUtils.hasText(tc)) {
                return tc;
            }
        }
        return null;
    }

    /**
     * 获取当前应用编码（从请求头 app-code 获取）
     */
    public static String getAppCode() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getHeader("app-code") : null;
    }

    /**
     * 获取当前 token（从请求头 Authorization 或 token 获取，去 Bearer 前缀）
     */
    public static String getToken() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            token = request.getHeader("token");
        }
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return token;
    }

    /**
     * 获取当前认证标识符（从 Session 获取）
     */
    public static String getAuthIdentifier() {
        Session s = getSession();
        return s != null ? s.getAuthIdentifier() : null;
    }

    // ── 路径匹配 ──

    /**
     * 路径匹配（Ant 风格）
     *
     * @param pattern 模式，如 "/*\/public\/**"
     * @param uri     请求 URI
     * @return true=匹配
     */
    public static boolean match(String pattern, String uri) {
        return PATH_MATCHER.match(pattern, uri);
    }


    private static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return null;
        }
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
        return servletRequestAttributes.getRequest();
    }


    public static Principal getPrincipal(HttpServletRequest request) {
        return request != null ? (Principal) request.getAttribute(ATTR_PRINCIPAL) : null;
    }

    public static Session getSession(HttpServletRequest request) {
        return request != null ? (Session) request.getAttribute(ATTR_SESSION) : null;
    }


}
