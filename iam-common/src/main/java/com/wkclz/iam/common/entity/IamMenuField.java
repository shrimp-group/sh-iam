package com.wkclz.iam.common.entity;

import com.wkclz.core.annotation.FieldDesc;
import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * Description Create by sh-generator
 *
 * @author shrimp
 * @table iam_menu_field (菜单字段关系) 重新生成代码会覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamMenuField extends BaseEntity {

    /**
     * 应用编码
     */
    @FieldDesc(value = "应用编码", notNull = true)
    private String appCode;

    /**
     * 菜单编码
     */
    @FieldDesc(value = "菜单编码", notNull = true)
    private String menuCode;

    /**
     * API字段权限编码
     */
    @FieldDesc(value = "API字段权限编码", notNull = true)
    private String fieldCode;


    public static IamMenuField copy(IamMenuField source, IamMenuField target) {
        if (target == null) {
            target = new IamMenuField();
        }
        if (source == null) {
            return target;
        }
        target.setId(source.getId());
        target.setAppCode(source.getAppCode());
        target.setMenuCode(source.getMenuCode());
        target.setFieldCode(source.getFieldCode());
        target.setSort(source.getSort());
        target.setCreateTime(source.getCreateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        target.setVersion(source.getVersion());
        return target;
    }

    public static IamMenuField copyIfNotNull(IamMenuField source, IamMenuField target) {
        if (target == null) {
            target = new IamMenuField();
        }
        if (source == null) {
            return target;
        }
        if (source.getId() != null) {
            target.setId(source.getId());
        }
        if (source.getAppCode() != null) {
            target.setAppCode(source.getAppCode());
        }
        if (source.getMenuCode() != null) {
            target.setMenuCode(source.getMenuCode());
        }
        if (source.getFieldCode() != null) {
            target.setFieldCode(source.getFieldCode());
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

