package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 角色列表查询请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "角色列表查询请求")
public class RoleListReq implements Serializable {

    @NotBlank(message = "appCode不能为空")
    @Schema(description = "应用编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @Schema(description = "租户编码")
    private String tenantCode;

    @Schema(description = "父角色编码")
    private String parentCode;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "是否可申请")
    private Integer applicable;

}
