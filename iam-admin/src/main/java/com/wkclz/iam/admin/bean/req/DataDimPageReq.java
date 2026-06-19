package com.wkclz.iam.admin.bean.req;

import com.wkclz.web.bean.PageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据维度分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "数据维度分页查询请求")
public class DataDimPageReq extends PageReq {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "维度名称")
    private String dimensionName;

    @Schema(description = "维度编码")
    private String dimensionCode;

}
