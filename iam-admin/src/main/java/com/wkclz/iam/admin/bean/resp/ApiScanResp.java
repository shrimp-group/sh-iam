package com.wkclz.iam.admin.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * API扫描结果响应项
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API扫描结果响应项")
public class ApiScanResp implements Serializable {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "模块")
    private String module;

    @Schema(description = "请求方法")
    private String apiMethod;

    @Schema(description = "请求URI")
    private String apiUri;

    @Schema(description = "API名称")
    private String apiName;

    @Schema(description = "白名单标识")
    private Integer writeFlag;

    @Schema(description = "是否已在库")
    private Boolean exists;

    @Schema(description = "已在库的API ID（exists=true 时有值）")
    private Long existingId;

}
