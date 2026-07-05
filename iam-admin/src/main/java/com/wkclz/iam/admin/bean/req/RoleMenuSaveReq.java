package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 角色-菜单保存请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "角色-菜单保存请求")
public class RoleMenuSaveReq implements Serializable {

    @NotBlank(message = "roleCode 不能为空")
    @Schema(description = "角色编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roleCode;

    @NotEmpty(message = "menuCodes 不能为空")
    @Schema(description = "菜单编码列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> menuCodes;

}
