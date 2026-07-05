package com.wkclz.iam.admin.bean.resp;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "应用响应")
public class AppResp extends EntityResp {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "应用域名")
    private String domain;

    @Schema(description = "鉴权类型")
    private String authType;

    @Schema(description = "图标")
    private String appIcon;

    @Schema(description = "登录页背景")
    private String loginBgp;

}
