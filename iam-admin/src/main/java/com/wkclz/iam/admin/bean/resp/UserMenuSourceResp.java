package com.wkclz.iam.admin.bean.resp;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 用户菜单来源响应（用于展示用户菜单的来源角色信息）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户菜单来源响应")
public class UserMenuSourceResp extends EntityResp {

    @Schema(description = "菜单编码")
    private String menuCode;

    @Schema(description = "菜单名称")
    private String menuName;

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
