package com.wkclz.iam.admin.bean.resp;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * API 详情响应（包含已绑定菜单全路径）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API详情响应")
public class ApiDetailResp extends EntityResp {

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

    @Schema(description = "已绑定的菜单全路径列表")
    private List<String> boundMenuPaths;

}
