package com.wkclz.iam.admin.bean.req;

import com.wkclz.web.bean.PageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "应用分页查询请求")
public class AppPageReq extends PageReq {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "应用域名")
    private String domain;

    @Schema(description = "鉴权类型")
    private String authType;

}
