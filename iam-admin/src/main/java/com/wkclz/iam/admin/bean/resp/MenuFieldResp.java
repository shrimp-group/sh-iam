package com.wkclz.iam.admin.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单字段绑定响应（含字段详情和API信息）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "菜单字段绑定响应")
public class MenuFieldResp {

    @Schema(description = "关联ID")
    private Long id;

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "菜单编码")
    private String menuCode;

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

    @Schema(description = "API编码")
    private String apiCode;

    @Schema(description = "请求方法")
    private String apiMethod;

    @Schema(description = "请求URI")
    private String apiUri;

    @Schema(description = "接口名称")
    private String apiName;

}
