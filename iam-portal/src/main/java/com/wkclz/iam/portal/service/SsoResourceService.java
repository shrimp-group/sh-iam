package com.wkclz.iam.portal.service;

import com.wkclz.iam.portal.entity.VueRouterMenu;
import com.wkclz.iam.portal.entity.VueRouterMeta;
import com.wkclz.iam.session.bean.auth.AuthMenu;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SSO 菜单资源服务 — 用户菜单树计算已由 AuthPermissionService（iam-session，三级缓存）承担，
 * 本类仅负责若依 VueRouter 格式转换。
 */
@Service
public class SsoResourceService {

    private static final Logger log = LoggerFactory.getLogger(SsoResourceService.class);

    /**
     * 获取若依格式的菜单树
     */
    public List<VueRouterMenu> getUserMenuTreeRuoyi(List<AuthMenu> menus) {
        log.info("开始构建若依格式菜单树，菜单数量: {}", menus.size());
        Map<String, List<String>> buttonsMap = new HashMap<>();
        Map<String, Integer> namesCount = new HashMap<>();
        List<VueRouterMenu> vueRouterMenus = menuTreeVueRouterTree(menus, null, buttonsMap, namesCount);
        log.info("若依格式菜单树构建完成，路由数量: {}", vueRouterMenus.size());
        return vueRouterMenus;
    }

    /**
     * 将菜单树转换为若依 VueRouter 菜单结构
     */
    private static List<VueRouterMenu> menuTreeVueRouterTree(List<AuthMenu> menus, String parentRoute, Map<String, List<String>> buttonsMap, Map<String, Integer> namesCount) {
        List<VueRouterMenu> vrMenus = new ArrayList<>();

        for (AuthMenu menu : menus) {
            if (!"MENU".equals(menu.getMenuType())) {
                continue;
            }
            parentRoute = StringUtils.isBlank(parentRoute) ? "" : parentRoute;
            String currentMenuRoute = StringUtils.isBlank(menu.getRoutePath()) ? "" : menu.getRoutePath();
            // 全路由用于映射按钮
            String currentRoute = currentMenuRoute.startsWith("/") ? currentMenuRoute : parentRoute + "/" + currentMenuRoute;
            VueRouterMenu vrMenu = new VueRouterMenu();
            vrMenus.add(vrMenu);
            vrMenu.setPath(currentMenuRoute);
            vrMenu.setName(path2Name(menu.getRoutePath(), namesCount));
            vrMenu.setHidden(menu.getHidden() != null && menu.getHidden() == 1);
            vrMenu.setRedirect(currentMenuRoute.isEmpty() || "/".equals(currentMenuRoute) ? "/index" : "noRedirect");
            vrMenu.setComponent(StringUtils.isBlank(menu.getComponent()) ? "error/index" : menu.getComponent());
            vrMenu.setAlwaysShow(false);
            if (!CollectionUtils.isEmpty(menu.getChildren())) {
                List<VueRouterMenu> children = menuTreeVueRouterTree(menu.getChildren(), currentRoute, buttonsMap, namesCount);
                vrMenu.setChildren(children);
            }

            // meta
            VueRouterMeta meta = new VueRouterMeta();
            meta.setTitle(menu.getMenuName());
            meta.setIcon(StringUtils.isBlank(menu.getIcon()) ? "form" : menu.getIcon());
            meta.setNoCache(false);
            vrMenu.setMeta(meta);

            // 按钮
            List<String> menuButtons = getMenuButtons(menu.getChildren());
            if (!CollectionUtils.isEmpty(menuButtons)) {
                buttonsMap.put(currentRoute, menuButtons);
            }
        }
        return vrMenus;
    }

    /**
     * 路由路径转组件名称（PascalCase，防重复）
     */
    private static String path2Name(String fullPath, Map<String, Integer> namesCount) {
        // 只保留字母、数字和斜杠
        String cleaned = fullPath.replaceAll("[^a-zA-Z0-9/]", "");
        // 按 '/' 分割，并过滤掉空片段
        String name = Arrays.stream(cleaned.split("/"))
                .filter(part -> !part.isEmpty())
                .map(t -> t.length() == 1 ? t.toUpperCase() : t.substring(0, 1).toUpperCase() + t.substring(1))
                .collect(Collectors.joining());
        if (StringUtils.isBlank(name)) {
            name = "_";
        }
        // 防止命名重复
        Integer i = namesCount.get(name);
        namesCount.put(name, i == null ? 1 : i + 1);
        return i == null ? name : name + "_" + i;
    }

    /**
     * 提取子菜单中的按钮编码
     */
    private static List<String> getMenuButtons(List<AuthMenu> menus) {
        if (CollectionUtils.isEmpty(menus)) {
            return null;
        }
        return menus.stream()
                .filter(t -> "BUTTON".equals(t.getMenuType()))
            .map(AuthMenu::getButtonCode)
                .filter(StringUtils::isNotBlank)
                .toList();
    }

}
