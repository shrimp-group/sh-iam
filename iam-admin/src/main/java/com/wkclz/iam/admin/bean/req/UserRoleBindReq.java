package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户-角色绑定请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户-角色绑定请求")
public class UserRoleBindReq {

    @NotBlank(message = "userCode 不能为空")
    @Schema(description = "用户编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userCode;

    @NotBlank(message = "roleCode 不能为空")
    @Schema(description = "角色编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roleCode;

    @NotNull(message = "startTime 不能为空")
    @Schema(description = "有效开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startTime;

    @NotNull(message = "endTime 不能为空")
    @Schema(description = "有效结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime endTime;

}
