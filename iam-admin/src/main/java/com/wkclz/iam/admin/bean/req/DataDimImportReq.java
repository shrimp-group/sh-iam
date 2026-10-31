package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 数据维度导入请求：按 dimensionCode 判断存在性，存在更新、不存在新增，全程无删除
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "数据维度导入请求")
public class DataDimImportReq implements Serializable {

    @NotEmpty(message = "维度列表不能为空")
    @Valid
    @Schema(description = "待导入的维度列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<DataDimImportItemReq> dims;

    /**
     * 数据维度导入项
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    @Schema(description = "数据维度导入项")
    public static class DataDimImportItemReq implements Serializable {

        @NotBlank(message = "应用编码不能为空")
        @Schema(description = "应用编码", requiredMode = Schema.RequiredMode.REQUIRED)
        private String appCode;

        @NotBlank(message = "维度编码不能为空")
        @Schema(description = "维度编码", requiredMode = Schema.RequiredMode.REQUIRED)
        private String dimensionCode;

        @NotBlank(message = "维度名称不能为空")
        @Schema(description = "维度名称", requiredMode = Schema.RequiredMode.REQUIRED)
        private String dimensionName;

        @Schema(description = "维度数据JSON(优先于脚本)")
        private String dimensionDataJson;

        @Schema(description = "维度脚本(优先级低)")
        private String dimensionScript;

        @Schema(description = "备注")
        private String remark;

        @Schema(description = "排序")
        private Integer sort;

    }

}
