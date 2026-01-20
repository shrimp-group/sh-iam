package com.wkclz.iam.common.entity;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;




/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_role_data (角色-数据关系) 重新生成代码会覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamRoleData extends BaseEntity {

    /**
     * 应用编码
     */
    @Desc("应用编码")
    private String appCode;

    /**
     * 角色编码
     */
    @Desc("角色编码")
    private String roleCode;

    /**
     * 数据维度编码
     */
    @Desc("数据维度编码")
    private String dimensionCode;

    /**
     * 数据维度
     */
    @Desc("数据维度")
    private String dataCode;


    public static IamRoleData copy(IamRoleData source, IamRoleData target) {
        if (target == null ) { target = new IamRoleData();}
        if (source == null) { return target; }
        target.setId(source.getId());
        target.setAppCode(source.getAppCode());
        target.setRoleCode(source.getRoleCode());
        target.setDimensionCode(source.getDimensionCode());
        target.setDataCode(source.getDataCode());
        target.setSort(source.getSort());
        target.setCreateTime(source.getCreateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        target.setVersion(source.getVersion());
        return target;
    }

    public static IamRoleData copyIfNotNull(IamRoleData source, IamRoleData target) {
        if (target == null ) { target = new IamRoleData();}
        if (source == null) { return target; }
        if (source.getId() != null) { target.setId(source.getId()); }
        if (source.getAppCode() != null) { target.setAppCode(source.getAppCode()); }
        if (source.getRoleCode() != null) { target.setRoleCode(source.getRoleCode()); }
        if (source.getDimensionCode() != null) { target.setDimensionCode(source.getDimensionCode()); }
        if (source.getDataCode() != null) { target.setDataCode(source.getDataCode()); }
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

