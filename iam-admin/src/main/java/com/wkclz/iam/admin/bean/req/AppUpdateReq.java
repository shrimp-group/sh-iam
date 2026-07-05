package com.wkclz.iam.admin.bean.req;

import com.wkclz.web.bean.UpdateReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用更新请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "应用更新请求")
public class AppUpdateReq extends UpdateReq {

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
