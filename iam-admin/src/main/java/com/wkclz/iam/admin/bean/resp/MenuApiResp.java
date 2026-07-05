package com.wkclz.iam.admin.bean.resp;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单-API关联响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "菜单-API关联响应")
public class MenuApiResp extends EntityResp {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "菜单编码")
    private String menuCode;

    @Schema(description = "API编码")
    private String apiCode;

}
