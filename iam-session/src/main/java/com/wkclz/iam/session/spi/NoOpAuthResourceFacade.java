package com.wkclz.iam.session.spi;

import com.wkclz.iam.session.bean.auth.AppAuthResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link AuthResourceFacade} 空实现 — 始终注册作为兜底，返回空资源（权限判断退化为全放行，不报错）。
 *
 * <p>注：不使用 {@code @ConditionalOnMissingBean} —— 该注解搭配 {@code @Component} 在自动配置的组件扫描中
 * 条件评估不可靠；上层模块（如 iam-portal 的 LocalAuthResourceFacade）通过实现类标注 {@code @Primary}
 * 覆盖默认行为。</p>
 */
@Slf4j
@Component
public class NoOpAuthResourceFacade implements AuthResourceFacade {

    @Override
    public AppAuthResource loadAppResource(String appCode) {
        log.debug("未接入权限数据源，返回空应用资源: appCode={}", appCode);
        return AppAuthResource.empty(appCode);
    }

    @Override
    public List<String> loadUserRoleCodes(String userCode) {
        log.debug("未接入权限数据源，返回空用户角色: userCode={}", userCode);
        return List.of();
    }
}
