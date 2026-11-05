package com.wkclz.iam.portal.service;

import com.wkclz.iam.common.entity.IamApp;
import com.wkclz.iam.portal.bean.resp.AppOptionResp;
import com.wkclz.iam.portal.config.IamPortalConfig;
import com.wkclz.iam.portal.mapper.SsoAppMapper;
import com.wkclz.tool.utils.BeanUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 门户应用列表服务 — 当前登录用户可访问应用汇总（TOKEN 应用 ∪ 用户生效角色应用）。
 *
 * <p>super 用户（配置 iam.portal.super-users）返回全部未删除应用。</p>
 *
 * @author shrimp
 */
@Slf4j
@Service
public class PortalAppService {

    private static final String AUTH_TYPE_TOKEN = "TOKEN";

    @Resource
    private SsoAppMapper ssoAppMapper;
    @Resource
    private IamPortalConfig config;

    public List<AppOptionResp> listAccessibleApps(String userCode) {
        log.info("查询可访问应用列表: userCode={}", userCode);
        try {
            if (StringUtils.isBlank(userCode)) {
                return new ArrayList<>();
            }
            List<IamApp> apps = ssoAppMapper.selectAllApps();
            // super 用户视为拥有全部应用可见权限；否则 TOKEN 应用恒展示 ∪ 用户有生效角色的应用
            Set<String> activeAppCodes = config.isSuperUser(userCode) ? null : activeAppCodes(userCode);
            List<IamApp> accessible = (apps == null ? new ArrayList<IamApp>() : apps).stream()
                .filter(app -> activeAppCodes == null || AUTH_TYPE_TOKEN.equals(app.getAuthType())
                    || activeAppCodes.contains(app.getAppCode()))
                // 按 sort 升序（null 排最后），按 appCode 去重
                .sorted(Comparator.comparing(IamApp::getSort, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toMap(IamApp::getAppCode, Function.identity(), (a, b) -> a, LinkedHashMap::new))
                .values().stream().toList();
            List<AppOptionResp> result = BeanUtil.cp(accessible, AppOptionResp.class);
            log.info("查询可访问应用列表完成: userCode={}, apps={}", userCode, result.size());
            return result;
        } catch (RuntimeException e) {
            log.error("查询可访问应用列表失败: userCode={}, error={}", userCode, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 用户有正在生效角色的应用编码（防御 null）
     */
    private Set<String> activeAppCodes(String userCode) {
        List<String> appCodes = ssoAppMapper.selectUserActiveAppCodes(userCode);
        return appCodes == null ? new LinkedHashSet<>() : new LinkedHashSet<>(appCodes);
    }
}
