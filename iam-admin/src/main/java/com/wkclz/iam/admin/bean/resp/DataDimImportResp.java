package com.wkclz.iam.admin.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 数据维度导入结果响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "数据维度导入结果响应")
public class DataDimImportResp implements Serializable {

    @Schema(description = "新增维度数")
    private Long insertCount;

    @Schema(description = "更新维度数")
    private Long updateCount;

}
