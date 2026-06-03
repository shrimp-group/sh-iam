package com.wkclz.iam.admin.bean.req;

import com.wkclz.web.bean.IdReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单-API 绑定请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "菜单-API绑定请求")
public class MenuApiBindReq extends IdReq {

    @NotBlank(message = "appCode 不能为空")
    @Schema(description = "应用编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @NotBlank(message = "menuCode 不能为空")
    @Schema(description = "菜单编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String menuCode;

    @NotBlank(message = "apiCode 不能为空")
    @Schema(description = "API编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String apiCode;

}
