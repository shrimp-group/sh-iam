package com.wkclz.iam.common.entity;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;




/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_menu (菜单) 重新生成代码会覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamMenu extends BaseEntity {

    /**
     * 应用编码
     */
    @Desc("应用编码")
    private String appCode;

    /**
     * 父编码，顶级为0
     */
    @Desc("父编码，顶级为0")
    private String parentCode;

    /**
     * 资源编码
     */
    @Desc("资源编码")
    private String menuCode;

    /**
     * 名称
     */
    @Desc("名称")
    private String menuName;

    /**
     * 图标
     */
    @Desc("图标")
    private String icon;

    /**
     * 菜单类型:菜单MENU, 按钮BUTTON
     */
    @Desc("菜单类型:菜单MENU, 按钮BUTTON")
    private String menuType;

    /**
     * 路由地址
     */
    @Desc("路由地址")
    private String routePath;

    /**
     * 组件
     */
    @Desc("组件")
    private String component;

    /**
     * 权限标识符
     */
    @Desc("权限标识符")
    private String buttonCode;

    /**
     * 隐藏
     */
    @Desc("隐藏")
    private Integer hidden;


    public static IamMenu copy(IamMenu source, IamMenu target) {
        if (target == null ) { target = new IamMenu();}
        if (source == null) { return target; }
        target.setId(source.getId());
        target.setAppCode(source.getAppCode());
        target.setParentCode(source.getParentCode());
        target.setMenuCode(source.getMenuCode());
        target.setMenuName(source.getMenuName());
        target.setIcon(source.getIcon());
        target.setMenuType(source.getMenuType());
        target.setRoutePath(source.getRoutePath());
        target.setComponent(source.getComponent());
        target.setButtonCode(source.getButtonCode());
        target.setHidden(source.getHidden());
        target.setSort(source.getSort());
        target.setCreateTime(source.getCreateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        target.setVersion(source.getVersion());
        return target;
    }

    public static IamMenu copyIfNotNull(IamMenu source, IamMenu target) {
        if (target == null ) { target = new IamMenu();}
        if (source == null) { return target; }
        if (source.getId() != null) { target.setId(source.getId()); }
        if (source.getAppCode() != null) { target.setAppCode(source.getAppCode()); }
        if (source.getParentCode() != null) { target.setParentCode(source.getParentCode()); }
        if (source.getMenuCode() != null) { target.setMenuCode(source.getMenuCode()); }
        if (source.getMenuName() != null) { target.setMenuName(source.getMenuName()); }
        if (source.getIcon() != null) { target.setIcon(source.getIcon()); }
        if (source.getMenuType() != null) { target.setMenuType(source.getMenuType()); }
        if (source.getRoutePath() != null) { target.setRoutePath(source.getRoutePath()); }
        if (source.getComponent() != null) { target.setComponent(source.getComponent()); }
        if (source.getButtonCode() != null) { target.setButtonCode(source.getButtonCode()); }
        if (source.getHidden() != null) { target.setHidden(source.getHidden()); }
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

