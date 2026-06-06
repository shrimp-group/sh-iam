package com.wkclz.iam.admin.bean.resp;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 角色-用户关系响应（用于角色详情页展示绑定用户）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "角色-用户关系响应")
public class RoleUserResp extends EntityResp {

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "有效开始时间")
    private LocalDateTime startTime;

    @Schema(description = "有效结束时间")
    private LocalDateTime endTime;

    @Schema(description = "当前有效状态")
    private Integer enableStatus;

}
