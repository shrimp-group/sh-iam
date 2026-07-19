package com.wkclz.iam.sso.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录请求。
 */
@Data
@Schema(description = "登录请求")
public class LoginReq implements Serializable {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "验证码")
    private String captchaCode;

    @Schema(description = "验证码ID")
    private String captchaId;
}
