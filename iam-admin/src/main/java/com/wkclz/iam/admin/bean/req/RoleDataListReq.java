package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 角色数据权限列表查询请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "角色数据权限列表查询请求")
public class RoleDataListReq implements Serializable {

    @NotBlank(message = "roleCode 不能为空")
    @Schema(description = "角色编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roleCode;

    @NotBlank(message = "dimensionCode 不能为空")
    @Schema(description = "数据维度编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dimensionCode;

}
