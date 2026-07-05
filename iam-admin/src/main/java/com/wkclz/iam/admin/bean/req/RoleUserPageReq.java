package com.wkclz.iam.admin.bean.req;

import com.wkclz.web.bean.PageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色下用户分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "角色下用户分页查询请求")
public class RoleUserPageReq extends PageReq {

    @NotBlank(message = "roleCode 不能为空")
    @Schema(description = "角色编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roleCode;

    @Schema(description = "用户名（精确匹配）")
    private String username;

    @Schema(description = "姓名（模糊匹配）")
    private String nickname;

}
