package com.wkclz.iam.portal.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "登录页应用信息")
public class SsoAppInfoResp {

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "登录页背景图")
    private String loginBgp;
}
