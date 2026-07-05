package com.wkclz.iam.admin.bean.req;

import com.wkclz.web.bean.PageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单-API 列表查询请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "菜单-API列表查询请求")
public class MenuApiListReq extends PageReq {

    @NotBlank(message = "menuCode 不能为空")
    @Schema(description = "菜单编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String menuCode;

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "搜索关键词（匹配URI或名称）")
    private String keyword;

}
