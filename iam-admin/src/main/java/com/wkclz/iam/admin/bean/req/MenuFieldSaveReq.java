package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 菜单字段批量保存请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "菜单字段批量保存请求")
public class MenuFieldSaveReq {

    @NotBlank(message = "appCode 不能为空")
    @Schema(description = "应用编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @NotBlank(message = "menuCode 不能为空")
    @Schema(description = "菜单编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String menuCode;

    @NotEmpty(message = "fieldCodes 不能为空")
    @Schema(description = "API字段权限编码列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> fieldCodes;

}
