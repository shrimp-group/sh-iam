package com.wkclz.iam.admin.bean.req;

import com.wkclz.iam.session.bean.ApiRequestControl;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * API导入请求：按 apiCode 判断存在性，存在更新、不存在新增，全程无删除
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API导入请求")
public class ApiImportReq implements Serializable {

    @NotEmpty(message = "API列表不能为空")
    @Valid
    @Schema(description = "待导入的API列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ApiImportItemReq> apis;

    /**
     * API导入项
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    @Schema(description = "API导入项")
    public static class ApiImportItemReq implements Serializable {

        @NotBlank(message = "应用编码不能为空")
        @Schema(description = "应用编码", requiredMode = Schema.RequiredMode.REQUIRED)
        private String appCode;

        @NotBlank(message = "接口编码不能为空")
        @Schema(description = "API编码", requiredMode = Schema.RequiredMode.REQUIRED)
        private String apiCode;

        @NotBlank(message = "请求方法不能为空")
        @Schema(description = "请求方法", requiredMode = Schema.RequiredMode.REQUIRED)
        private String apiMethod;

        @NotBlank(message = "请求URI不能为空")
        @Schema(description = "请求URI", requiredMode = Schema.RequiredMode.REQUIRED)
        private String apiUri;

        @Schema(description = "模块")
        private String module;

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

        @Valid
        @Schema(description = "字段权限列表")
        private List<ApiImportFieldReq> fields;

    }

    /**
     * API字段权限导入项：fieldCode 为数据权限精准匹配标识，必填，不允许后端生成
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    @Schema(description = "API字段权限导入项")
    public static class ApiImportFieldReq implements Serializable {

        @NotBlank(message = "字段编码不能为空")
        @Schema(description = "字段编码", requiredMode = Schema.RequiredMode.REQUIRED)
        private String fieldCode;

        @NotBlank(message = "字段名称不能为空")
        @Schema(description = "字段名称", requiredMode = Schema.RequiredMode.REQUIRED)
        private String fieldName;

        @NotBlank(message = "JSON路径不能为空")
        @Schema(description = "JSONPath表达式", requiredMode = Schema.RequiredMode.REQUIRED)
        private String jsonPath;

        @NotBlank(message = "权限动作不能为空")
        @Schema(description = "动作类型(HIDDEN/MASK/READ_ONLY)", requiredMode = Schema.RequiredMode.REQUIRED)
        private String action;

        @Schema(description = "脱敏规则(keepHead,keepTail)")
        private String maskRule;

        @Schema(description = "描述说明")
        private String description;

        @Schema(description = "排序")
        private Integer sort;

    }

}
