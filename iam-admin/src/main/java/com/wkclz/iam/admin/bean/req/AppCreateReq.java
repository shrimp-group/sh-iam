package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 应用创建请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "应用创建请求")
public class AppCreateReq implements Serializable {

    @NotBlank(message = "应用编码不能为空")
    @Schema(description = "应用编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @NotBlank(message = "应用名称不能为空")
    @Schema(description = "应用名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appName;

    @NotBlank(message = "应用域名不能为空")
    @Schema(description = "应用域名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String domain;

    @NotBlank(message = "鉴权类型不能为空")
    @Schema(description = "鉴权类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String authType;

    @Schema(description = "图标")
    private String appIcon;

    @Schema(description = "登录页背景")
    private String loginBgp;

    @Schema(description = "备注")
    private String remark;

}
