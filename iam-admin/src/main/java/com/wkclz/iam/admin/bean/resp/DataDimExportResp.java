package com.wkclz.iam.admin.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 数据维度导出响应：仅含业务字段（不含主键与审计字段），导入时按 dimensionCode 定位
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "数据维度导出响应")
public class DataDimExportResp implements Serializable {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "维度编码")
    private String dimensionCode;

    @Schema(description = "维度名称")
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
