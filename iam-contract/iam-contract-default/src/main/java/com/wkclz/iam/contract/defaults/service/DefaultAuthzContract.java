package com.wkclz.iam.contract.defaults.service;

import com.wkclz.iam.contract.bean.App;
import com.wkclz.iam.contract.bean.DataDimension;
import com.wkclz.iam.contract.bean.FieldPermission;
import com.wkclz.auth.bean.MenuNode;
import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.bean.Tenant;
import com.wkclz.iam.contract.enums.AuthErrorType;
import com.wkclz.iam.contract.exception.AuthException;
import com.wkclz.iam.contract.service.AuthzContract;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * 鉴权契约默认实现
 * 读宽容：返回空列表 / 原字段（不影响启动）
 * 验证严格：canAccessApi 抛 ACCESS_DENIED（防止裸奔）
 *
 * @author shrimp
 */
@Slf4j
public class DefaultAuthzContract implements AuthzContract {

    // ── 读操作：返回空/原值 ──

    @Override
    public List<Tenant> listTenants(Principal principal) {
        log.debug("DefaultAuthzContract: listTenants 无实现，返回空列表");
        return Collections.emptyList();
    }

    @Override
    public List<App> listApps(Principal principal, String tenantCode) {
        log.debug("DefaultAuthzContract: listApps 无实现，返回空列表");
        return Collections.emptyList();
    }

    @Override
    public List<MenuNode> getMenuTree(Principal principal, String appCode) {
        log.debug("DefaultAuthzContract: getMenuTree 无实现，返回空列表");
        return Collections.emptyList();
    }

    @Override
    public List<FieldPermission> listFieldPermissions(Principal principal, String appCode, String menuCode) {
        log.debug("DefaultAuthzContract: listFieldPermissions 无实现，返回空列表");
        return Collections.emptyList();
    }

    @Override
    public List<String> filterFields(Principal principal, String appCode, String menuCode, List<String> fields) {
        // 字段过滤无实现 → 不过滤，返回原字段列表
        return fields;
    }

    @Override
    public List<DataDimension> getDataDimensions(Principal principal, String appCode) {
        log.debug("DefaultAuthzContract: getDataDimensions 无实现，返回空列表");
        return Collections.emptyList();
    }

    // ── 验证操作：严格拒绝 ──

    @Override
    public boolean canAccessApi(Principal principal, String appCode, String apiUri, String apiMethod) {
        log.warn("DefaultAuthzContract: canAccessApi 无实现，默认拒绝。apiUri={}, method={}", apiUri, apiMethod);
        throw new AuthException(AuthErrorType.ACCESS_DENIED,
                "无鉴权实现，请配置 AuthzContract");
    }
}
