package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户认证重置密码请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户认证重置密码请求")
public class UserAuthResetPasswordReq implements Serializable {

    @NotBlank(message = "用户编码不能为空")
    @Schema(description = "用户编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userCode;

    @NotBlank(message = "新密码不能为空")
    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

}
