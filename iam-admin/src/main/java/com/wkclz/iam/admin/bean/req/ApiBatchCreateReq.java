package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * API批量新增请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API批量新增请求")
public class ApiBatchCreateReq implements Serializable {

    @NotEmpty(message = "API列表不能为空")
    @Valid
    @Schema(description = "待新增的API列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ApiBatchCreateItemReq> items;

    /**
     * API批量新增项
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    @Schema(description = "API批量新增项")
    public static class ApiBatchCreateItemReq implements Serializable {

        @Schema(description = "模块")
        private String module;

        @Schema(description = "应用编码")
        private String appCode;

        @Schema(description = "请求方法")
        private String apiMethod;

        @Schema(description = "请求URI")
        private String apiUri;

        @Schema(description = "API名称")
        private String apiName;

        @Schema(description = "白名单标识")
        private Integer writeFlag;

        @Schema(description = "排序")
        private Integer sort;

        @Schema(description = "备注")
        private String remark;

    }

}
