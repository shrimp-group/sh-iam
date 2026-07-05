package com.wkclz.iam.admin.bean.req;

import com.wkclz.web.bean.UpdateReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API更新请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API更新请求")
public class ApiUpdateReq extends UpdateReq {

    @Schema(description = "模块")
    private String module;

    @NotBlank(message = "应用编码不能为空")
    @Schema(description = "应用编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @Schema(description = "API编码")
    private String apiCode;

    @NotBlank(message = "请求方法不能为空")
    @Schema(description = "请求方法", requiredMode = Schema.RequiredMode.REQUIRED)
    private String apiMethod;

    @NotBlank(message = "请求URI不能为空")
    @Schema(description = "请求URI", requiredMode = Schema.RequiredMode.REQUIRED)
    private String apiUri;

    @Schema(description = "API名称")
    private String apiName;

    @Schema(description = "白名单标识")
    private Integer writeFlag;

    @Schema(description = "备注")
    private String remark;

}
