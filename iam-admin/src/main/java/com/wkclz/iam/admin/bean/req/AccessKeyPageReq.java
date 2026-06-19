package com.wkclz.iam.admin.bean.req;

import com.wkclz.web.bean.PageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 访问密钥分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "访问密钥分页查询请求")
public class AccessKeyPageReq extends PageReq {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "应用ID")
    private String appId;

    @Schema(description = "生效状态")
    private Integer enableStatus;

}
