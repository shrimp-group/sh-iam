package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 数据维度导出请求：ids 有值时按主键批量导出（选中项），否则按筛选条件全量导出（与页面查询同语义，无分页）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "数据维度导出请求")
public class DataDimExportReq implements Serializable {

    @Schema(description = "主键ID列表（导出选中项时使用，优先于筛选条件）")
    private List<Long> ids;

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "维度编码")
    private String dimensionCode;

    @Schema(description = "维度名称")
    private String dimensionName;

}
