package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 角色数据权限绑定请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "角色数据权限绑定请求")
public class RoleDataBindReq implements Serializable {

    @NotBlank(message = "appCode 不能为空")
    @Schema(description = "应用编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @NotBlank(message = "roleCode 不能为空")
    @Schema(description = "角色编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roleCode;

    @NotBlank(message = "dimensionCode 不能为空")
    @Schema(description = "数据维度编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dimensionCode;

    @NotBlank(message = "dataCode 不能为空")
    @Schema(description = "数据维度值", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dataCode;

}
