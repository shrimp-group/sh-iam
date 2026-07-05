package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * API选项查询请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API选项查询请求")
public class ApiListReq implements Serializable {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "请求方法")
    private String apiMethod;

    @Schema(description = "请求URI")
    private String apiUri;

}
