package com.wkclz.iam.portal.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 可访问应用选项响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "可访问应用选项响应")
public class AppOptionResp {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "应用域名")
    private String domain;

}
