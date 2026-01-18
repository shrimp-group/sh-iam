package com.wkclz.iam.common.entity;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;




/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_user_role (用户-角色关系) 重新生成代码会覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamUserRole extends BaseEntity {

    /**
     * 租户编码
     */
    @Desc("租户编码")
    private String tenantCode;

    /**
     * 应用编码
     */
    @Desc("应用编码")
    private String appCode;

    /**
     * 用户编码
     */
    @Desc("用户编码")
    private String userCode;

    /**
     * 角色编码
     */
    @Desc("角色编码")
    private String roleCode;


    public static IamUserRole copy(IamUserRole source, IamUserRole target) {
        if (target == null ) { target = new IamUserRole();}
        if (source == null) { return target; }
        target.setId(source.getId());
        target.setTenantCode(source.getTenantCode());
        target.setAppCode(source.getAppCode());
        target.setUserCode(source.getUserCode());
        target.setRoleCode(source.getRoleCode());
        target.setSort(source.getSort());
        target.setCreateTime(source.getCreateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        target.setVersion(source.getVersion());
        return target;
    }

    public static IamUserRole copyIfNotNull(IamUserRole source, IamUserRole target) {
        if (target == null ) { target = new IamUserRole();}
        if (source == null) { return target; }
        if (source.getId() != null) { target.setId(source.getId()); }
        if (source.getTenantCode() != null) { target.setTenantCode(source.getTenantCode()); }
        if (source.getAppCode() != null) { target.setAppCode(source.getAppCode()); }
        if (source.getUserCode() != null) { target.setUserCode(source.getUserCode()); }
        if (source.getRoleCode() != null) { target.setRoleCode(source.getRoleCode()); }
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

