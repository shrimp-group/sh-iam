package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * API导出请求：ids 有值时按主键批量导出（选中项），否则按筛选条件全量导出（与页面查询同语义，无分页）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API导出请求")
public class ApiExportReq implements Serializable {

    @Schema(description = "主键ID列表（导出选中项时使用，优先于筛选条件）")
    private List<Long> ids;

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

    @Schema(description = "菜单绑定状态：1-已绑定，0-未绑定")
    private Integer menuBindStatus;

}
