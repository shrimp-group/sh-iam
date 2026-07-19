package com.wkclz.iam.common.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import com.wkclz.core.base.BaseEntity;
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
     * 可用状态
     */
    @Schema(description = "可用状态")
    private Integer enableFlag;

    /**
     * 可用开始
     */
    @Schema(description = "可用开始")
    private LocalDateTime enableBegin;

    /**
     * 可用结束
     */
    @Schema(description = "可用结束")
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
        target.setEnableFlag(source.getEnableFlag());
        target.setEnableBegin(source.getEnableBegin());
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
        if (source.getEnableFlag() != null) {
            target.setEnableFlag(source.getEnableFlag());
        }
        if (source.getEnableBegin() != null) {
            target.setEnableBegin(source.getEnableBegin());
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

