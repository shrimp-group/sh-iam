package com.wkclz.iam.portal.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 密码过期自助修改请求 — 免登录态，靠用户名 + 旧密码 + 图片验证码建立身份信任。
 */
@Data
@Schema(description = "密码过期自助修改请求")
public class ExpiredPasswordChangeReq implements Serializable {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "旧密码不能为空")
    @Schema(description = "旧密码（RSA 密文）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Schema(description = "新密码（RSA 密文）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;

    @NotBlank(message = "验证码ID不能为空")
    @Schema(description = "验证码ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private String captchaId;

    @NotBlank(message = "验证码不能为空")
    @Schema(description = "验证码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String captchaCode;
}
