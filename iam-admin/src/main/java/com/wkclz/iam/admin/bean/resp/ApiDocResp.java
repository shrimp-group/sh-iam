package com.wkclz.iam.admin.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "接口文档响应")
public class ApiDocResp {

    @Schema(description = "请求方法")
    private String method;

    @Schema(description = "请求URI")
    private String uri;

    @Schema(description = "接口名称")
    private String name;

    @Schema(description = "接口描述")
    private String description;

    @Schema(description = "请求参数列表")
    private List<ApiDocParamResp> requestParams;

    @Schema(description = "返回参数字段树")
    private List<EntityFieldNode> returnFields;

    @Schema(description = "类级别 @Tag 描述")
    private String tag;

    @Schema(description = "@Operation(summary)")
    private String operationSummary;

    @Schema(description = "@Operation(description)")
    private String operationDescription;

    @Schema(description = "接口是否废弃")
    private Boolean deprecated;

    @Schema(description = "请求 Content-Type")
    private String[] consumes;

    @Schema(description = "响应 Content-Type")
    private String[] produces;

    @Schema(description = "返回参数字段树（基于 RestField 结构）")
    private List<ApiDocFieldResp> returnSchemaFields;
}
