package com.wkclz.iam.admin.bean.resp;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API 绑定信息响应（用于菜单详情页穿梭框展示）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API绑定信息响应")
public class ApiBoundResp extends EntityResp {

    @Schema(description = "模块")
    private String module;

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "API编码")
    private String apiCode;

    @Schema(description = "请求方法")
    private String apiMethod;

    @Schema(description = "请求URI")
    private String apiUri;

    @Schema(description = "API名称")
    private String apiName;

    @Schema(description = "白名单标识")
    private Integer writeFlag;

    @Schema(description = "菜单-API关联记录ID（用于解绑）")
    private Long menuApiId;

}
