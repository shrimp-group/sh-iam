package com.wkclz.iam.admin.bean.req;

import com.wkclz.web.bean.UpdateReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户更新请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户更新请求")
public class UserUpdateReq extends UpdateReq {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "昵称不能为空")
    @Schema(description = "昵称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "状态：1-启用，2-禁用，3-锁定")
    private Integer userStatus;

    @Schema(description = "应用编码")
    private String appCode;

}
