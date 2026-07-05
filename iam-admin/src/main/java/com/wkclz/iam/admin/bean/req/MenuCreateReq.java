package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 菜单创建请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "菜单创建请求")
public class MenuCreateReq implements Serializable {

    @NotBlank(message = "appCode不能为空")
    @Schema(description = "应用编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @Schema(description = "父编码")
    private String parentCode;

    @Schema(description = "菜单编码")
    private String menuCode;

    @NotBlank(message = "菜单名称不能为空")
    @Schema(description = "菜单名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String menuName;

    @NotBlank(message = "菜单类型不能为空")
    @Schema(description = "菜单类型:菜单MENU, 按钮BUTTON, 字段组FIELDS", requiredMode = Schema.RequiredMode.REQUIRED)
    private String menuType;

    @Schema(description = "路由地址")
    private String routePath;

    @Schema(description = "组件")
    private String component;

    @Schema(description = "按钮编码")
    private String buttonCode;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "隐藏")
    private Integer hidden;

    @Schema(description = "排序")
    private Integer sort;

}
