package com.wkclz.iam.common.entity;

import com.wkclz.core.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;


/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_tenant (租户) 重新生成代码会覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamTenant extends BaseEntity {

    /**
     * 租户编码
     */
    @Schema(description = "租户编码")
    private String tenantCode;

    /**
     * 租户名称
     */
    @Schema(description = "租户名称")
    private String tenantName;

    /**
     * 可用状态：1-启用，0-禁用
     */
    @Schema(description = "可用状态：1-启用，0-禁用")
    private Integer enableStatus;

    /**
     * 可用开始
     */
    @Schema(description = "可用开始")
    private LocalDateTime enableStart;

    /**
     * 可用结束(NULL表示永不过期)
     */
    @Schema(description = "可用结束(NULL表示永不过期)")
    private LocalDateTime enableEnd;


    public static IamTenant copy(IamTenant source, IamTenant target) {
        if (target == null) {
            target = new IamTenant();
        }
        if (source == null) {
            return target;
        }
        target.setId(source.getId());
        target.setTenantCode(source.getTenantCode());
        target.setTenantName(source.getTenantName());
        target.setEnableStatus(source.getEnableStatus());
        target.setEnableStart(source.getEnableStart());
        target.setEnableEnd(source.getEnableEnd());
        target.setSort(source.getSort());
        target.setCreateTime(source.getCreateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        target.setVersion(source.getVersion());
        return target;
    }

    public static IamTenant copyIfNotNull(IamTenant source, IamTenant target) {
        if (target == null) {
            target = new IamTenant();
        }
        if (source == null) {
            return target;
        }
        if (source.getId() != null) {
            target.setId(source.getId());
        }
        if (source.getTenantCode() != null) {
            target.setTenantCode(source.getTenantCode());
        }
        if (source.getTenantName() != null) {
            target.setTenantName(source.getTenantName());
        }
        if (source.getEnableStatus() != null) {
            target.setEnableStatus(source.getEnableStatus());
        }
        if (source.getEnableStart() != null) {
            target.setEnableStart(source.getEnableStart());
        }
        if (source.getEnableEnd() != null) {
            target.setEnableEnd(source.getEnableEnd());
        }
        if (source.getSort() != null) {
            target.setSort(source.getSort());
        }
        if (source.getCreateTime() != null) {
            target.setCreateTime(source.getCreateTime());
        }
        if (source.getCreateBy() != null) {
            target.setCreateBy(source.getCreateBy());
        }
        if (source.getUpdateTime() != null) {
            target.setUpdateTime(source.getUpdateTime());
        }
        if (source.getUpdateBy() != null) {
            target.setUpdateBy(source.getUpdateBy());
        }
        if (source.getRemark() != null) {
            target.setRemark(source.getRemark());
        }
        if (source.getVersion() != null) {
            target.setVersion(source.getVersion());
        }
        return target;
    }

}

