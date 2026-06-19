package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 访问密钥创建请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "访问密钥创建请求")
public class AccessKeyCreateReq implements Serializable {

    @NotBlank(message = "应用编码不能为空")
    @Schema(description = "应用编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @NotBlank(message = "应用ID不能为空")
    @Schema(description = "应用ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appId;

    @Schema(description = "密钥")
    private String secretKey;

    @Schema(description = "生效状态")
    private Integer enableStatus;

    @Schema(description = "描述")
    private String description;

}
