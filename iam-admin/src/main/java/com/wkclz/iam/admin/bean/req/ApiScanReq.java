package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * API扫描请求（支持后端过滤，减少返回数据量）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API扫描请求")
public class ApiScanReq implements Serializable {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "模块")
    private String module;

    @Schema(description = "请求方法")
    private String apiMethod;

    @Schema(description = "请求URI（模糊匹配）")
    private String apiUri;

    @Schema(description = "接口名称（模糊匹配）")
    private String apiName;

    @Schema(description = "入库状态：true-仅已入库，false-仅可新增，null-全部")
    private Boolean exists;

}
