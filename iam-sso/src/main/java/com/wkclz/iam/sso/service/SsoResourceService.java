package com.wkclz.iam.sso.service;

import com.wkclz.iam.common.dto.IamMenuDto;
import com.wkclz.iam.common.entity.IamMenu;
import com.wkclz.iam.sso.entity.VueRouterMenu;
import com.wkclz.iam.sso.entity.VueRouterMeta;
import com.wkclz.iam.sso.mapper.SsoResourceMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SsoResourceService {

    private static final Logger log = LoggerFactory.getLogger(SsoResourceService.class);

    @Autowired
    private SsoResourceMapper ssoResourceMapper;

    public List<IamMenuDto> getUserMenuList(String appCode) {
        return ssoResourceMapper.getUserMenu(appCode);
    }

    public List<IamMenuDto> getUserMenuTree(String appCode) {
        List<IamMenuDto> userMenu = ssoResourceMapper.getUserMenu(appCode);
        return makeTree(userMenu);
    }

    /**
     * 获取若依格式的菜单树
     */
    public List<VueRouterMenu> getUserMenuTreeRuoyi(List<IamMenuDto> menus) {
        log.info("开始构建若依格式菜单树，菜单数量: {}", menus.size());
        Map<String, List<String>> buttonsMap = new HashMap<>();
        Map<String, Integer> namesCount = new HashMap<>();
        List<VueRouterMenu> vueRouterMenus = menuTreeVueRouterTree(menus, null, buttonsMap, namesCount);
        log.info("若依格式菜单树构建完成，路由数量: {}", vueRouterMenus.size());
        return vueRouterMenus;
    }

    private static List<IamMenuDto> makeTree(List<IamMenuDto> dtos) {
        List<IamMenuDto> tree = new ArrayList<>();
        for (IamMenuDto l : dtos) {
            if ("0".equals(l.getParentCode())) {
                tree.add(l);
            } else {
                for (IamMenuDto p : dtos) {
                    if (p.getMenuCode().equals(l.getParentCode())) {
                        List<IamMenuDto> children = p.getChildren();
                        if (children == null) {
                            children = new ArrayList<>();
                        }
                        children.add(l);
                        p.setChildren(children);
                    }
                }
            }
        }
        return tree;
    }

    /**
     * 将菜单树转换为若依 VueRouter 菜单结构
     */
    private static List<VueRouterMenu> menuTreeVueRouterTree(List<IamMenuDto> menus, String parentRoute, Map<String, List<String>> buttonsMap, Map<String, Integer> namesCount) {
        List<VueRouterMenu> vrMenus = new ArrayList<>();

        for (IamMenuDto menu : menus) {
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
    private static List<String> getMenuButtons(List<IamMenuDto> menus) {
        if (CollectionUtils.isEmpty(menus)) {
            return null;
        }
        return menus.stream()
                .filter(t -> "BUTTON".equals(t.getMenuType()))
                .map(IamMenu::getButtonCode)
                .filter(StringUtils::isNotBlank)
                .toList();
    }

}
