package com.wkclz.iam.sdk.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "登录请求")
public class LoginReq implements Serializable {

    /**
     * 用户名 【用户名/手机号/邮箱账号/工号, 需根据具体特点进行区分】
     */
    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    /**
     * 密码 【密码，短信验证码，邮件验证码 等值】
     */
    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "验证码")
    private String captchaCode;

    @Schema(description = "验证码ID")
    private String captchaId;

}
