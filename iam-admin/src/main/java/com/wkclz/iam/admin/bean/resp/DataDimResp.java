package com.wkclz.iam.admin.bean.resp;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据维度响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "数据维度响应")
public class DataDimResp extends EntityResp {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "维度名称")
    private String dimensionName;

    @Schema(description = "维度编码")
    private String dimensionCode;

    @Schema(description = "描述")
    private String description;

}
