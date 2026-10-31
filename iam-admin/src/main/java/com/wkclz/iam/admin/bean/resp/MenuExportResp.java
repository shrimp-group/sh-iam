package com.wkclz.iam.admin.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单导出响应：树形嵌套菜单节点，顶层仅为菜单数组；节点含全部业务字段、apiCodes（仅接口编码）与递归 children，
 * 不携带接口定义、字段权限、角色-菜单关联等其他数据
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "菜单导出响应")
public class MenuExportResp implements Serializable {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "父编码，顶级为0")
    private String parentCode;

    @Schema(description = "菜单编码")
    private String menuCode;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "菜单类型:菜单MENU, 按钮BUTTON, 字段组FIELDS")
    private String menuType;

    @Schema(description = "路由地址")
    private String routePath;

    @Schema(description = "组件")
    private String component;

    @Schema(description = "权限标识符")
    private String buttonCode;

    @Schema(description = "隐藏：0-否，1-是")
    private Integer hidden;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "绑定的接口编码列表，无绑定时为空数组")
    private List<String> apiCodes;

    @Schema(description = "子菜单，递归结构同父节点")
    private List<MenuExportResp> children;

}
