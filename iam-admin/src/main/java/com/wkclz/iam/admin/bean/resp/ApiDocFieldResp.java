package com.wkclz.iam.admin.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "接口文档字段响应")
public class ApiDocFieldResp {

    @Schema(description = "唯一标识（基于路径）")
    private String key;

    @Schema(description = "字段名称")
    private String name;

    @Schema(description = "字段类型（完整类名）")
    private String type;

    @Schema(description = "是否为简单类型")
    private Boolean simpleType;

    @Schema(description = "字段描述（来自 @Schema.description）")
    private String description;

    @Schema(description = "示例值（来自 @Schema.example）")
    private String example;

    @Schema(description = "是否必填")
    private Boolean required;

    @Schema(description = "泛型参数类型列表")
    private List<String> genericTypes;

    @Schema(description = "子字段（递归）")
    private List<ApiDocFieldResp> fields;

    @Schema(description = "是否自引用类型")
    private Boolean selfReferencing;
}
