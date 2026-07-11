package com.wkclz.iam.contract.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * API 路由
 *
 * @author shrimp
 */
@Data
@Schema(description = "API 路由")
public class Api implements Serializable {

    @Schema(description = "API 编码")
    private String apiCode;

    @Schema(description = "API 名称")
    private String apiName;

    @Schema(description = "HTTP 方法：GET / POST / PUT / DELETE")
    private String apiMethod;

    @Schema(description = "URI 路径")
    private String apiUri;

    @Schema(description = "是否写操作")
    private Boolean writeFlag;
}
