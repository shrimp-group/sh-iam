package com.wkclz.iam.common.entity;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;




/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_menu_api (菜单 接口) 重新生成代码会覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamMenuApi extends BaseEntity {

    /**
     * 所属应用
     */
    @Desc("所属应用")
    private String appCode;

    /**
     * 菜单编码
     */
    @Desc("菜单编码")
    private String menuCode;

    /**
     * API 编码
     */
    @Desc("API 编码")
    private String apiCode;


    public static IamMenuApi copy(IamMenuApi source, IamMenuApi target) {
        if (target == null ) { target = new IamMenuApi();}
        if (source == null) { return target; }
        target.setId(source.getId());
        target.setAppCode(source.getAppCode());
        target.setMenuCode(source.getMenuCode());
        target.setApiCode(source.getApiCode());
        target.setSort(source.getSort());
        target.setCreateTime(source.getCreateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        target.setVersion(source.getVersion());
        return target;
    }

    public static IamMenuApi copyIfNotNull(IamMenuApi source, IamMenuApi target) {
        if (target == null ) { target = new IamMenuApi();}
        if (source == null) { return target; }
        if (source.getId() != null) { target.setId(source.getId()); }
        if (source.getAppCode() != null) { target.setAppCode(source.getAppCode()); }
        if (source.getMenuCode() != null) { target.setMenuCode(source.getMenuCode()); }
        if (source.getApiCode() != null) { target.setApiCode(source.getApiCode()); }
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

