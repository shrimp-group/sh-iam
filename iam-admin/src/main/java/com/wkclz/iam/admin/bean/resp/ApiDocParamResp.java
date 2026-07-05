package com.wkclz.iam.admin.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "接口文档参数响应")
public class ApiDocParamResp {

    @Schema(description = "参数名")
    private String name;

    @Schema(description = "参数类型")
    private String type;

    @Schema(description = "注解类型: RequestBody/PathVariable/RequestParam/Parameter")
    private String annotationType;

    @Schema(description = "是否必需")
    private Boolean required;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "子字段树（复杂类型展开）")
    private List<EntityFieldNode> children;

    @Schema(description = "参数示例值（来自 @Schema.example）")
    private String example;

    @Schema(description = "必填模式（来自 @Schema.requiredMode）")
    private String requiredMode;

    @Schema(description = "参数字段树（基于 RestField 结构）")
    private List<ApiDocFieldResp> fields;
}
