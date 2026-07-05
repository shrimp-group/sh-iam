package com.wkclz.iam.admin.bean.resp;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API字段权限响应（含关联API信息）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API字段权限响应")
public class ApiFieldResp extends EntityResp {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "API编码")
    private String apiCode;

    @Schema(description = "字段编码")
    private String fieldCode;

    @Schema(description = "字段名称")
    private String fieldName;

    @Schema(description = "JSONPath表达式")
    private String jsonPath;

    @Schema(description = "动作类型")
    private String action;

    @Schema(description = "脱敏规则")
    private String maskRule;

    @Schema(description = "描述说明")
    private String description;

    @Schema(description = "请求方法")
    private String apiMethod;

    @Schema(description = "请求URI")
    private String apiUri;

    @Schema(description = "接口名称")
    private String apiName;

}
