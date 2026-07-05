package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户菜单查询请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户菜单查询请求")
public class UserMenuListReq implements Serializable {

    @NotBlank(message = "appCode 不能为空")
    @Schema(description = "应用编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @Schema(description = "菜单编码")
    private String menuCode;

}
