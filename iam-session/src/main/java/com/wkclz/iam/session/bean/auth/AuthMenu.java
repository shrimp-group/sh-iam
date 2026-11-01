package com.wkclz.iam.session.bean.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 菜单 lite 模型（iam-session 模块自持轻量模型，不依赖 iam-common 实体），
 * MENU 与 BUTTON 两类共用（对应 iam_menu 表 menu_type 列）。
 *
 * @author shrimp
 */
@Data
public class AuthMenu {

    /**
     * 菜单编码
     */
    @Schema(description = "菜单编码")
    private String menuCode;

    /**
     * 父菜单编码（根为 0）
     */
    @Schema(description = "父菜单编码")
    private String parentCode;

    /**
     * 菜单名称
     */
    @Schema(description = "菜单名称")
    private String menuName;

    /**
     * 类型：MENU-菜单 / BUTTON-按钮
     */
    @Schema(description = "类型：MENU-菜单 / BUTTON-按钮")
    private String menuType;

    /**
     * 路由地址
     */
    @Schema(description = "路由地址")
    private String routePath;

    /**
     * 组件路径
     */
    @Schema(description = "组件路径")
    private String component;

    /**
     * 按钮编码（menuType=BUTTON 时有值）
     */
    @Schema(description = "按钮编码")
    private String buttonCode;

    /**
     * 图标
     */
    @Schema(description = "图标")
    private String icon;

    /**
     * 是否隐藏：1-隐藏
     */
    @Schema(description = "是否隐藏：1-隐藏")
    private Integer hidden;

    /**
     * 排序
     */
    @Schema(description = "排序")
    private Integer sort;

    /**
     * 子节点（组树结果，静态资源加载时为 null）
     */
    @Schema(description = "子节点")
    private List<AuthMenu> children;
}
