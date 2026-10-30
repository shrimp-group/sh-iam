package com.wkclz.iam.admin.bean.resp;

import com.wkclz.iam.session.bean.ApiRequestControl;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * API导出响应：仅含业务字段（不含主键与审计字段），导入时按 apiCode 定位、按 (apiCode+fieldCode) 定位字段权限
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API导出响应")
public class ApiExportResp implements Serializable {

    @Schema(description = "模块")
    private String module;

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "API编码")
    private String apiCode;

    @Schema(description = "请求方法")
    private String apiMethod;

    @Schema(description = "请求URI")
    private String apiUri;

    @Schema(description = "API名称")
    private String apiName;

    @Schema(description = "白名单标识")
    private Integer writeFlag;

    @Schema(description = "请求控制配置")
    private ApiRequestControl requestControl;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "字段权限列表")
    private List<ApiFieldExportResp> fields;

    /**
     * API字段权限导出响应
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    @Schema(description = "API字段权限导出响应")
    public static class ApiFieldExportResp implements Serializable {

        @Schema(description = "字段编码")
        private String fieldCode;

        @Schema(description = "字段名称")
        private String fieldName;

        @Schema(description = "JSONPath表达式")
        private String jsonPath;

        @Schema(description = "动作类型(HIDDEN/MASK/READ_ONLY)")
        private String action;

        @Schema(description = "脱敏规则(keepHead,keepTail)")
        private String maskRule;

        @Schema(description = "描述说明")
        private String description;

        @Schema(description = "排序")
        private Integer sort;

    }

}
