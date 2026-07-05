package com.wkclz.iam.admin.bean.resp;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 菜单-用户关系响应（用于菜单详情页展示关联用户）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "菜单-用户关系响应")
public class MenuUserResp extends EntityResp {

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "来源角色编码")
    private String roleCode;

    @Schema(description = "来源角色名称")
    private String roleName;

    @Schema(description = "有效开始时间")
    private LocalDateTime startTime;

    @Schema(description = "有效结束时间")
    private LocalDateTime endTime;

    @Schema(description = "当前有效状态")
    private Integer enableStatus;

}
