package com.wkclz.iam.common.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;




/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_role (角色) 重新生成代码会覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamRole extends BaseEntity {

    /**
     * 租户编码
     */
    @Schema(description = "租户编码")
    private String tenantCode;

    /**
     * 应用编码
     */
    @Schema(description = "应用编码")
    private String appCode;

    /**
     * 父角色
     */
    @Schema(description = "父角色")
    private String parentCode;

    /**
     * 角色编码
     */
    @Schema(description = "角色编码")
    private String roleCode;

    /**
     * 名称
     */
    @Schema(description = "名称")
    private String roleName;

    /**
     * 是否可申请
     */
    @Schema(description = "是否可申请")
    private Integer applicable;


    public static IamRole copy(IamRole source, IamRole target) {
        if (target == null ) { target = new IamRole();}
        if (source == null) { return target; }
        target.setId(source.getId());
        target.setTenantCode(source.getTenantCode());
        target.setAppCode(source.getAppCode());
        target.setParentCode(source.getParentCode());
        target.setRoleCode(source.getRoleCode());
        target.setRoleName(source.getRoleName());
        target.setApplicable(source.getApplicable());
        target.setSort(source.getSort());
        target.setCreateTime(source.getCreateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        target.setVersion(source.getVersion());
        return target;
    }

    public static IamRole copyIfNotNull(IamRole source, IamRole target) {
        if (target == null ) { target = new IamRole();}
        if (source == null) { return target; }
        if (source.getId() != null) { target.setId(source.getId()); }
        if (source.getTenantCode() != null) { target.setTenantCode(source.getTenantCode()); }
        if (source.getAppCode() != null) { target.setAppCode(source.getAppCode()); }
        if (source.getParentCode() != null) { target.setParentCode(source.getParentCode()); }
        if (source.getRoleCode() != null) { target.setRoleCode(source.getRoleCode()); }
        if (source.getRoleName() != null) { target.setRoleName(source.getRoleName()); }
        if (source.getApplicable() != null) {
            target.setApplicable(source.getApplicable());
        }
        if (source.getSort() != null) { target.setSort(source.getSort()); }
        if (source.getCreateTime() != null) { target.setCreateTime(source.getCreateTime()); }
        if (source.getCreateBy() != null) { target.setCreateBy(source.getCreateBy()); }
        if (source.getUpdateTime() != null) { target.setUpdateTime(source.getUpdateTime()); }
        if (source.getUpdateBy() != null) { target.setUpdateBy(source.getUpdateBy()); }
        if (source.getRemark() != null) { target.setRemark(source.getRemark()); }
        if (source.getVersion() != null) { target.setVersion(source.getVersion()); }
        return target;
    }

}

