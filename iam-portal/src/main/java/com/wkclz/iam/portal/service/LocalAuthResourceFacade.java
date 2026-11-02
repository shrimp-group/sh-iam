package com.wkclz.iam.portal.service;

import com.alibaba.fastjson2.JSON;
import com.wkclz.iam.common.entity.IamApi;
import com.wkclz.iam.common.entity.IamMenuApi;
import com.wkclz.iam.common.entity.IamRoleMenu;
import com.wkclz.iam.portal.mapper.SsoAuthResourceMapper;
import com.wkclz.iam.session.bean.ApiRequestControl;
import com.wkclz.iam.session.bean.auth.AppAuthResource;
import com.wkclz.iam.session.bean.auth.AuthApi;
import com.wkclz.iam.session.bean.auth.AuthApiField;
import com.wkclz.iam.session.bean.auth.AuthApp;
import com.wkclz.iam.session.bean.auth.AuthMenu;
import com.wkclz.iam.session.spi.AuthResourceFacade;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AuthResourceFacade 本地实现 — co-deploy 形态数据供给方。
 *
 * <p>以 {@code @Primary} 覆盖 iam-session 始终注册的 {@code NoOpAuthResourceFacade}
 * （装配模式：NoOp 始终注册，@Primary 覆盖，不使用 @ConditionalOnMissingBean）；
 * 将 {@link SsoAuthResourceMapper} 的 8 条按应用查询装配为 {@link AppAuthResource}（按键索引的静态快照）。</p>
 *
 * <p>查询异常直接向上抛出（RuntimeException），由缓存调用方 fail-open 处理；
 * 关系指向应用外/已删除 API 时跳过该关系并记 warn，不阻断整体加载。</p>
 *
 * @author shrimp
 */
@Slf4j
@Component
@Primary
public class LocalAuthResourceFacade implements AuthResourceFacade {

    @Resource
    private SsoAuthResourceMapper ssoAuthResourceMapper;

    @Override
    public AppAuthResource loadAppResource(String appCode) {
        log.info("加载应用静态资源: appCode={}", appCode);
        try {
            AuthApp app = ssoAuthResourceMapper.selectApp(appCode);
            Set<String> roleCodes = ssoAuthResourceMapper.listAppRoles(appCode);
            List<AuthMenu> menus = ssoAuthResourceMapper.listAppMenus(appCode);
            List<IamRoleMenu> roleMenus = ssoAuthResourceMapper.listRoleMenus(appCode);
            List<AuthApi> apis = ssoAuthResourceMapper.listAppApis(appCode);
            List<IamMenuApi> menuApis = ssoAuthResourceMapper.listMenuApis(appCode);
            List<AuthApiField> apiFields = ssoAuthResourceMapper.listApiFields(appCode);
            List<IamApi> controlApis = ssoAuthResourceMapper.listRequestControlApis(appCode);

            AppAuthResource resource = new AppAuthResource();
            resource.setAppCode(appCode);
            resource.setApp(app);

            // 应用角色（防御 null）
            resource.setRoleCodes(roleCodes == null ? new LinkedHashSet<>() : new LinkedHashSet<>(roleCodes));

            // menuMap：LinkedHashMap 保 sort 序（menuCode → 节点）
            Map<String, AuthMenu> menuMap = menus == null ? new LinkedHashMap<>()
                : menus.stream().collect(Collectors.toMap(AuthMenu::getMenuCode, Function.identity(),
                (a, b) -> a, LinkedHashMap::new));
            resource.setMenuMap(menuMap);

            // roleMenuMap：roleCode → menuCodes（一对多）
            Map<String, Set<String>> roleMenuMap = new HashMap<>();
            if (roleMenus != null) {
                for (IamRoleMenu rel : roleMenus) {
                    roleMenuMap.computeIfAbsent(rel.getRoleCode(), k -> new HashSet<>()).add(rel.getMenuCode());
                }
            }
            resource.setRoleMenuMap(roleMenuMap);

            // apiMap：apiCode → 定义（LinkedHashMap 保加载序）
            Map<String, AuthApi> apiMap = apis == null ? new LinkedHashMap<>()
                : apis.stream().collect(Collectors.toMap(AuthApi::getApiCode, Function.identity(),
                (a, b) -> a, LinkedHashMap::new));
            resource.setApiMap(apiMap);

            // menuApiMap：menuCode → apiCodes（一对多，与 roleMenuMap 同构）；关系 apiCode 不在 apiMap 时跳过
            Map<String, Set<String>> menuApiMap = new HashMap<>();
            if (menuApis != null) {
                for (IamMenuApi rel : menuApis) {
                    if (!apiMap.containsKey(rel.getApiCode())) {
                        log.warn("菜单API关系指向应用外或已删除的API定义，跳过: appCode={}, menuCode={}, apiCode={}",
                            appCode, rel.getMenuCode(), rel.getApiCode());
                        continue;
                    }
                    menuApiMap.computeIfAbsent(rel.getMenuCode(), k -> new HashSet<>()).add(rel.getApiCode());
                }
            }
            resource.setMenuApiMap(menuApiMap);

            // 接口字段权限
            resource.setApiFields(apiFields == null ? new ArrayList<>() : apiFields);

            // requestControls：逐条解析（空白跳过、解析失败 warn 跳过）
            List<AppAuthResource.ApiControlEntry> requestControls = new ArrayList<>();
            if (controlApis != null) {
                for (IamApi api : controlApis) {
                    if (StringUtils.isBlank(api.getRequestControl())) {
                        continue;
                    }
                    try {
                        ApiRequestControl control = JSON.parseObject(api.getRequestControl(), ApiRequestControl.class);
                        if (control != null) {
                            requestControls.add(new AppAuthResource.ApiControlEntry(
                                api.getApiCode(), api.getApiMethod(), api.getApiUri(), control));
                        }
                    } catch (Exception e) {
                        log.warn("请求控制配置解析失败，跳过: appCode={}, apiCode={}, error={}",
                            appCode, api.getApiCode(), e.getMessage());
                    }
                }
            }
            resource.setRequestControls(requestControls);

            resource.setLoadTime(System.currentTimeMillis());
            log.info("加载应用静态资源完成: appCode={}, appName={}, authType={}, roles={}, menus={}, apis={}, roleMenuRels={}, menuApiRels={}, requestControls={}",
                appCode, app == null ? null : app.getAppName(), app == null ? null : app.getAuthType(),
                resource.getRoleCodes().size(), menuMap.size(), apiMap.size(),
                roleMenuMap.values().stream().mapToInt(Set::size).sum(),
                menuApiMap.values().stream().mapToInt(Set::size).sum(), requestControls.size());
            return resource;
        } catch (RuntimeException e) {
            log.error("加载应用静态资源失败: appCode={}, error={}", appCode, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public List<String> loadUserRoleCodes(String userCode) {
        log.info("加载用户有效角色: userCode={}", userCode);
        try {
            List<String> roleCodes = ssoAuthResourceMapper.listUserActiveRoleCodes(userCode);
            List<String> result = roleCodes == null ? new ArrayList<>() : roleCodes;
            log.info("加载用户有效角色完成: userCode={}, roles={}", userCode, result.size());
            return result;
        } catch (RuntimeException e) {
            log.error("加载用户有效角色失败: userCode={}, error={}", userCode, e.getMessage(), e);
            throw e;
        }
    }
}
