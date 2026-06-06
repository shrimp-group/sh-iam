package com.wkclz.iam.admin.bean.resp;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户-角色关系响应（用于用户详情页展示绑定角色）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户-角色关系响应")
public class UserRoleResp extends EntityResp {

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "有效开始时间")
    private LocalDateTime startTime;

    @Schema(description = "有效结束时间")
    private LocalDateTime endTime;

    @Schema(description = "当前有效状态")
    private Integer enableStatus;

}
