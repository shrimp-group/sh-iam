package com.wkclz.iam.session.service;

import com.wkclz.core.exception.SystemException;
import com.wkclz.iam.session.bean.auth.AppAuthResource;
import com.wkclz.iam.session.bean.auth.AuthApi;
import com.wkclz.iam.session.bean.auth.AuthApp;
import com.wkclz.iam.session.bean.auth.AuthMenu;
import com.wkclz.iam.session.cache.AuthCacheManager;
import com.wkclz.iam.session.enums.AppAuthType;
import com.wkclz.iam.session.spi.AuthResourceFacade;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.util.*;

/**
 * 权限计算服务 — 低频菜单树计算（每次直接从 L1+L2 计算，不缓存结果）、
 * 高频按 URL 接口权限判断（经 {@link AuthCacheManager#hasUrlPermission} 走 L3 缓存 true/false）、
 * 接口白名单判定（DB 白名单，fail-closed 于豁免）与纳管判定（精确索引 + 通配小列表），
 * 以及按应用鉴权类型判断 requireApiPermission（TOKEN 免接口权限校验，缺失/未知默认 TOKEN_API fail-closed）。
 *
 * <p><b>缓存快照不可变</b>：{@link AppAuthResource} 内对象（menuMap/roleMenuMap/apiMap/menuApiMap 等）
 * 为多请求共享实例，任何计算不得修改其字段 —— 组树必须浅拷贝节点，严禁对 menuMap 内实例 setChildren
 * （否则并发请求互相污染树结构）。</p>
 *
 * <p><b>匹配规则统一</b>：method 忽略大小写 + apiUri Ant 通配匹配请求 URI。</p>
 *
 * @author shrimp
 */
@Slf4j
@Service
public class AuthPermissionService {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Resource
    private AuthCacheManager manager;


    /**
     * 计算用户菜单树：有效角色（绑定 ∪ 公共；super 为全部角色）→ 角色菜单汇总 → 祖先链闭包 → 浅拷贝组树。
     *
     * <p>组树规则：parentCode 为 0/null 为根；父节点不在可见集合内的孤儿菜单升为根；
     * 顺序取 menuMap 迭代序（加载时已按 sort, id 排序）；BUTTON 类型天然仅保留已绑定者。</p>
     *
     * @param userCode 用户编码
     * @param appCode  应用编码
     * @return 菜单树（浅拷贝节点，children 仅属于本次计算结果）
     */
    public List<AuthMenu> getUserMenuTree(String userCode, String appCode) {
        log.info("计算用户菜单树: userCode={}, appCode={}", userCode, appCode);
        try {
            AppAuthResource res = manager.getAppResource(appCode);
            List<String> userRoles = manager.getUserRoleCodes(userCode);
            Set<String> boundMenuCodes = collectBoundMenuCodes(res, userRoles);

            // 祖先链闭包：沿 parentCode 向上补齐祖先
            Set<String> visibleCodes = new LinkedHashSet<>(boundMenuCodes);
            for (String menuCode : boundMenuCodes) {
                AuthMenu node = res.getMenuMap().get(menuCode);
                while (node != null) {
                    String parentCode = node.getParentCode();
                    if (parentCode == null || "0".equals(parentCode)) {
                        break;
                    }
                    // 祖先已在集合说明该链已补齐，无需继续
                    if (!visibleCodes.add(parentCode)) {
                        break;
                    }
                    node = res.getMenuMap().get(parentCode);
                }
            }

            // 浅拷贝组树：按 menuMap 迭代序保 sort 序，children 仅属于本次结果
            Map<String, AuthMenu> copied = new LinkedHashMap<>();
            for (Map.Entry<String, AuthMenu> entry : res.getMenuMap().entrySet()) {
                if (visibleCodes.contains(entry.getKey())) {
                    copied.put(entry.getKey(), shallowCopy(entry.getValue()));
                }
            }
            List<AuthMenu> roots = new ArrayList<>();
            for (AuthMenu node : copied.values()) {
                String parentCode = node.getParentCode();
                AuthMenu parent = (parentCode == null || "0".equals(parentCode)) ? null : copied.get(parentCode);
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    // 根节点；父不在可见集合的孤儿菜单升为根
                    roots.add(node);
                }
            }
            log.info("计算用户菜单树完成: userCode={}, appCode={}, roots={}, nodes={}",
                userCode, appCode, roots.size(), copied.size());
            return roots;
        } catch (RuntimeException e) {
            log.error("计算用户菜单树失败: userCode={}, appCode={}", userCode, appCode, e);
            throw new SystemException("计算用户菜单树失败: userCode=" + userCode + ", appCode=" + appCode, e);
        }
    }

    /**
     * 判断用户对指定接口是否有权限（L3 按 URL 缓存 true/false，未命中单点计算后写回）。
     *
     * <p>fail-open：计算链路任何异常（缓存/加载失败等）log.error 后放行，
     * 与既有请求控制策略一致。</p>
     *
     * @param appCode  应用编码
     * @param userCode 用户编码
     * @param method   HTTP 方法
     * @param uri      请求 URI
     * @return 是否有权限（异常时放行 true）
     */
    public boolean hasApiPermission(String appCode, String userCode, String method, String uri) {
        String urlKey = (method == null ? "" : method.toUpperCase()) + ":" + uri;
        try {
            return manager.hasUrlPermission(appCode, userCode, urlKey,
                () -> computeSinglePermission(appCode, userCode, method, uri));
        } catch (Exception e) {
            log.error("接口权限计算异常，fail-open 放行: appCode={}, userCode={}, method={}, uri={}",
                appCode, userCode, method, uri, e);
            return true;
        }
    }

    /**
     * 判断接口是否需要权限校验：
     * 合并部署（ownerKey={@link AuthCacheManager#GLOBAL_KEY}）按全局配置 {@code iam.auth.auth-type}；
     * 独立部署按应用 auth_type；应用缺失/未知按 TOKEN_API 安全默认（fail-closed）。
     *
     * @param ownerKey 鉴权资源键（应用编码或 GLOBAL）
     * @return 需要校验返回 true（TOKEN 返回 false）
     */
    public boolean requireApiPermission(String ownerKey) {
        if (AuthCacheManager.GLOBAL_KEY.equals(ownerKey)) {
            return AppAuthType.of(manager.getConfig().getAuthType()).requireApiPermission();
        }
        AppAuthResource res = manager.getAppResource(ownerKey);
        AuthApp app = res.getApp();
        return AppAuthType.of(app == null ? null : app.getAuthType()).requireApiPermission();
    }

    /**
     * 判断接口是否被应用纳管（任一菜单绑定的 API 匹配请求即已纳管）。
     *
     * <p>纳管索引判定：精确键（大写 METHOD:URI）O(1) 优先，未命中遍历通配小列表（URI 含通配的 API）；
     * 匹配语义与遍历实现一致（method 忽略大小写 + uri Ant）。</p>
     *
     * @param appCode 应用编码
     * @param method  HTTP 方法
     * @param uri     请求 URI
     * @return 已纳管返回 true；未纳管返回 false（过滤器放行）
     */
    public boolean isManagedApi(String appCode, String method, String uri) {
        log.info("判断接口是否纳管: appCode={}, method={}, uri={}", appCode, method, uri);
        AppAuthResource res = manager.getAppResource(appCode);
        if (res.getManagedExactKeys().contains((method == null ? "" : method.toUpperCase()) + ":" + uri)) {
            return true;
        }
        for (AuthApi api : res.getManagedWildcardApis()) {
            if (matchApi(api, method, uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断接口是否命中 DB 白名单（{@code iam_api.write_flag=1}，免登录免接口权限校验）。
     *
     * <p>白名单索引判定：精确键（大写 METHOD:URI）O(1) 优先，未命中遍历通配小列表；
     * 匹配语义与纳管一致（method 忽略大小写 + uri Ant）。</p>
     *
     * <p>fail-closed 于豁免：判定链路异常（如 L1 加载失败）视为非白名单，继续走原鉴权链路。</p>
     *
     * @param ownerKey 鉴权资源键（应用编码或 GLOBAL）
     * @param method   HTTP 方法
     * @param uri      请求 URI
     * @return 白名单命中返回 true；异常或未命中返回 false
     */
    public boolean isApiWhiteListed(String ownerKey, String method, String uri) {
        try {
            AppAuthResource res = manager.getAppResource(ownerKey);
            if (res.getWhiteListExactKeys().contains((method == null ? "" : method.toUpperCase()) + ":" + uri)) {
                return true;
            }
            for (AuthApi api : res.getWhiteListWildcardApis()) {
                if (matchApi(api, method, uri)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("接口白名单判定异常，fail-closed 于豁免: ownerKey={}, method={}, uri={}", ownerKey, method, uri, e);
            return false;
        }
    }

    /**
     * 提取用户可见按钮编码（供前端按钮权限控制）。
     *
     * @param userCode 用户编码
     * @param appCode  应用编码
     * @return 按钮编码列表
     */
    public List<String> getUserButtonCodes(String userCode, String appCode) {
        List<String> buttonCodes = new ArrayList<>();
        collectButtonCodes(getUserMenuTree(userCode, appCode), buttonCodes);
        log.info("提取用户按钮编码完成: userCode={}, appCode={}, count={}", userCode, appCode, buttonCodes.size());
        return buttonCodes;
    }

    /**
     * 单点计算用户对指定接口的权限（L3 未命中时执行）。
     */
    private boolean computeSinglePermission(String appCode, String userCode, String method, String uri) {
        AppAuthResource res = manager.getAppResource(appCode);
        List<String> userRoles = manager.getUserRoleCodes(userCode);
        Set<String> boundMenuCodes = collectBoundMenuCodes(res, userRoles);
        if (boundMenuCodes.isEmpty()) {
            return false;
        }
        for (String menuCode : boundMenuCodes) {
            Set<String> apiCodes = res.getMenuApiMap().get(menuCode);
            if (apiCodes == null || apiCodes.isEmpty()) {
                continue;
            }
            for (String apiCode : apiCodes) {
                AuthApi api = res.getApiMap().get(apiCode);
                if (api != null && matchApi(api, method, uri)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 汇总用户可见菜单编码：有效角色 → roleMenuMap 逐角色合并。
     *
     * <p>有效角色规则：super 用户（{@link AuthResourceFacade#SUPER_ROLE_CODE} 哨兵）拥有应用内全部角色；
     * 普通用户 = 绑定角色 ∪ 应用公共角色（无需申请，访问即生效）。</p>
     */
    private Set<String> collectBoundMenuCodes(AppAuthResource res, List<String> userRoles) {
        Set<String> boundMenuCodes = new LinkedHashSet<>();
        if (userRoles.contains(AuthResourceFacade.SUPER_ROLE_CODE)) {
            for (Set<String> menuCodes : res.getRoleMenuMap().values()) {
                boundMenuCodes.addAll(menuCodes);
            }
            return boundMenuCodes;
        }
        Set<String> effectiveRoles = new LinkedHashSet<>(userRoles);
        if (res.getPublicRoleCodes() != null) {
            effectiveRoles.addAll(res.getPublicRoleCodes());
        }
        for (String roleCode : effectiveRoles) {
            if (!res.getRoleCodes().contains(roleCode)) {
                continue;
            }
            Set<String> menuCodes = res.getRoleMenuMap().get(roleCode);
            if (menuCodes != null && !menuCodes.isEmpty()) {
                boundMenuCodes.addAll(menuCodes);
            }
        }
        return boundMenuCodes;
    }

    /**
     * API 匹配判定：method 匹配（apiMethod 配置为 REQUEST 时通配任意请求方法，否则忽略大小写相等）
     * + apiUri Ant 通配匹配请求 URI。
     */
    private boolean matchApi(AuthApi api, String method, String uri) {
        if (api.getApiMethod() == null || api.getApiUri() == null) {
            return false;
        }
        boolean methodMatched = "REQUEST".equalsIgnoreCase(api.getApiMethod()) || api.getApiMethod().equalsIgnoreCase(method);
        return methodMatched && pathMatcher.match(api.getApiUri(), uri);
    }

    /**
     * 浅拷贝菜单节点（children 仅属于本次计算结果，严禁复用缓存内实例）。
     */
    private AuthMenu shallowCopy(AuthMenu src) {
        AuthMenu copy = new AuthMenu();
        copy.setMenuCode(src.getMenuCode());
        copy.setParentCode(src.getParentCode());
        copy.setMenuName(src.getMenuName());
        copy.setMenuType(src.getMenuType());
        copy.setRoutePath(src.getRoutePath());
        copy.setComponent(src.getComponent());
        copy.setButtonCode(src.getButtonCode());
        copy.setIcon(src.getIcon());
        copy.setHidden(src.getHidden());
        copy.setSort(src.getSort());
        copy.setChildren(new ArrayList<>());
        return copy;
    }

    private void collectButtonCodes(List<AuthMenu> nodes, List<String> result) {
        for (AuthMenu node : nodes) {
            if ("BUTTON".equals(node.getMenuType()) && node.getButtonCode() != null) {
                result.add(node.getButtonCode());
            }
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                collectButtonCodes(node.getChildren(), result);
            }
        }
    }
}
