package com.wkclz.iam.sso.rest;

import com.wkclz.core.base.R;
import com.wkclz.iam.common.dto.IamMenuDto;
import com.wkclz.iam.common.entity.IamMenu;
import com.wkclz.iam.sdk.helper.SessionHelper;
import com.wkclz.iam.sdk.model.UserSession;
import com.wkclz.iam.sso.Route;
import com.wkclz.iam.sso.entity.VueRouterMenu;
import com.wkclz.iam.sso.entity.VueRouterMeta;
import com.wkclz.iam.sso.service.IamLoginService;
import com.wkclz.iam.sso.service.SsoResourceService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author shrimp
 */
@RestController
@RequestMapping(Route.PREFIX)
public class UserInfoRest {

    @Autowired
    private IamLoginService iamLoginService;
    @Autowired
    private SsoResourceService ssoResourceService;

    @GetMapping(Route.USER_INFO)
    public R publicSsoLogin(HttpServletRequest request) {
        UserSession userSession = SessionHelper.getUserSession(request);
        return R.ok(userSession);
    }

    @GetMapping(Route.USER_MENU_TREE)
    public R<List<IamMenuDto>> userMenuTree(HttpServletRequest request) {
        String appCode = SessionHelper.getAppCode(request);
        if (StringUtils.isBlank(appCode)) {
            return R.error("appCode is blank in Headers");
        }
        List<IamMenuDto> tree = ssoResourceService.getUserMenuTree(appCode);
        return R.ok(tree);
    }

    @GetMapping(Route.USER_MENU_TREE_RUOYI)
    public R userMenuTreeRuoyi(HttpServletRequest request) {
        R<List<IamMenuDto>> listR = userMenuTree(request);
        List<IamMenuDto> menus = listR.getData();

        // 加工成 Ruoyi 的菜单结构
        Map<String, List<String>> buttonsMap = new HashMap<>();
        Map<String, Integer> namesCount = new HashMap<>();

        List<VueRouterMenu> vueRouterMenus = menuTreeVueRouterTree(menus, null, buttonsMap, namesCount);
        return R.ok(vueRouterMenus);
    }


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
            vrMenu.setRedirect(currentMenuRoute.isEmpty() || "/".equals(currentMenuRoute) ? "/index": "noRedirect");
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

    private static String path2Name(String fullPath, Map<String, Integer> namesCount) {
        // 1. 只保留字母、数字和斜杠
        String cleaned = fullPath.replaceAll("[^a-zA-Z0-9/]", "");
        // 2. 按 '/' 分割，并过滤掉空片段
        String name = Arrays.stream(cleaned.split("/"))
                .filter(part -> !part.isEmpty())
                .map(t -> t.length() == 1 ? t.toUpperCase() : t.substring(0, 1).toUpperCase() + t.substring(1))
                .collect(Collectors.joining());
        if (StringUtils.isBlank(name)) {
            name = "_";
        }
        // 3. 防止命名重复
        Integer i = namesCount.get(name);
        namesCount.put(name, i == null ? 1 : i + 1);
        return i == null ? name : name + "_" + i;
    }

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
