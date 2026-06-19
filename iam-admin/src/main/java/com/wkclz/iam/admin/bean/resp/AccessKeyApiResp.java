package com.wkclz.iam.admin.bean.resp;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AK-API关联响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "AK-API关联响应")
public class AccessKeyApiResp extends EntityResp {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "应用ID")
    private String appId;

    @Schema(description = "API编码")
    private String apiCode;

}
