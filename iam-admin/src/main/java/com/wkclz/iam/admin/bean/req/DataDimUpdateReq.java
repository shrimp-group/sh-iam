package com.wkclz.iam.admin.bean.req;

import com.wkclz.web.bean.UpdateReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据维度更新请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "数据维度更新请求")
public class DataDimUpdateReq extends UpdateReq {

    @NotBlank(message = "应用编码不能为空")
    @Schema(description = "应用编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @NotBlank(message = "维度名称不能为空")
    @Schema(description = "维度名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dimensionName;

    @Schema(description = "维度编码")
    private String dimensionCode;

    @Schema(description = "描述")
    private String description;

}
