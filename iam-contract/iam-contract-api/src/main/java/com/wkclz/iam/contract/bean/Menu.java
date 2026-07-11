package com.wkclz.iam.contract.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单（树形结构）
 * 仅包含核心展示字段，不含管理字段；树构建由实现层负责
 *
 * @author shrimp
 */
@Deprecated
@Data
@Schema(description = "菜单")
public class Menu implements Serializable {

    @Schema(description = "菜单编码")
    private String menuCode;

    @Schema(description = "父级菜单编码")
    private String parentCode;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单类型：MENU / BUTTON")
    private String menuType;

    @Schema(description = "路由路径")
    private String routePath;

    @Schema(description = "前端组件路径")
    private String component;

    @Schema(description = "按钮编码（menuType=BUTTON 时）")
    private String buttonCode;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "子菜单列表")
    private List<Menu> children;
}
