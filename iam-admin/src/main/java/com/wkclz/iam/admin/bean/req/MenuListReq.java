package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 菜单列表查询请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "菜单列表查询请求")
public class MenuListReq implements Serializable {

    @NotBlank(message = "appCode不能为空")
    @Schema(description = "应用编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @Schema(description = "父编码")
    private String parentCode;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单类型:菜单MENU, 按钮BUTTON, 字段组FIELDS")
    private String menuType;

    @Schema(description = "菜单编码")
    private String menuCode;

}
