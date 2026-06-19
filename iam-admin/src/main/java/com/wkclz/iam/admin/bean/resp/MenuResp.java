package com.wkclz.iam.admin.bean.resp;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 菜单响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "菜单响应")
public class MenuResp extends EntityResp {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "父编码")
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

    @Schema(description = "按钮编码")
    private String buttonCode;

    @Schema(description = "隐藏")
    private Integer hidden;

    @Schema(description = "子菜单列表")
    private List<MenuResp> children;

}
