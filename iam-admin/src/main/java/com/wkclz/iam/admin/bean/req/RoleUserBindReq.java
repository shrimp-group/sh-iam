package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色-用户绑定请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "角色-用户绑定请求")
public class RoleUserBindReq {

    @NotBlank(message = "roleCode 不能为空")
    @Schema(description = "角色编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roleCode;

    @NotEmpty(message = "userCodes 不能为空")
    @Schema(description = "用户编码列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> userCodes;

    @NotNull(message = "startTime 不能为空")
    @Schema(description = "有效开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startTime;

    @NotNull(message = "endTime 不能为空")
    @Schema(description = "有效结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime endTime;

}
