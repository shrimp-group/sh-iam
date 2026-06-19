package com.wkclz.iam.admin.bean.req;

import com.wkclz.web.bean.UpdateReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色更新请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "角色更新请求")
public class RoleUpdateReq extends UpdateReq {

    @NotBlank(message = "appCode不能为空")
    @Schema(description = "应用编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String appCode;

    @NotBlank(message = "角色名称不能为空")
    @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roleName;

    @Schema(description = "父角色编码")
    private String parentCode;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "是否可申请")
    private Integer applicable;

}
