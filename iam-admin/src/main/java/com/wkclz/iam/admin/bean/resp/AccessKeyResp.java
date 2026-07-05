package com.wkclz.iam.admin.bean.resp;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 访问密钥响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "访问密钥响应")
public class AccessKeyResp extends EntityResp {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "应用ID")
    private String appId;

    @Schema(description = "密钥")
    private String secretKey;

    @Schema(description = "生效状态")
    private Integer enableStatus;

    @Schema(description = "描述")
    private String description;

}
