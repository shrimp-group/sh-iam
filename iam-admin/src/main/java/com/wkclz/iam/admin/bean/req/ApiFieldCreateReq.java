package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * API字段权限创建请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API字段权限创建请求")
public class ApiFieldCreateReq implements Serializable {

    @NotBlank(message = "appCode 不能为空")
    @Schema(description = "应用编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @NotBlank(message = "apiCode 不能为空")
    @Schema(description = "API编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String apiCode;

    @Schema(description = "字段编码")
    private String fieldCode;

    @NotBlank(message = "fieldName 不能为空")
    @Schema(description = "字段名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fieldName;

    @Schema(description = "JSONPath表达式")
    private String jsonPath;

    @NotBlank(message = "action 不能为空")
    @Schema(description = "动作类型(HIDDEN/MASK/READ_ONLY)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String action;

    @Schema(description = "脱敏规则(keepHead,keepTail)")
    private String maskRule;

    @Schema(description = "描述说明")
    private String description;

}
