package com.wkclz.auth.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 应用
 *
 * @author shrimp
 */
@Data
@Schema(description = "应用")
public class App implements Serializable {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "应用图标")
    private String icon;
}
