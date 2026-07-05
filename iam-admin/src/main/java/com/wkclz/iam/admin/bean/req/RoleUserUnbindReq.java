package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 角色-用户解绑请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "角色-用户解绑请求")
public class RoleUserUnbindReq {

    @NotEmpty(message = "ids 不能为空")
    @Schema(description = "用户-角色关联ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;

}
