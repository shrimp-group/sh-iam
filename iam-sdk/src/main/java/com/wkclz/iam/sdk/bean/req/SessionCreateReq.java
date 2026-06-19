package com.wkclz.iam.sdk.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 会话创建请求，用于使用用户信息创建会话并记录登录日志
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "会话创建请求")
public class SessionCreateReq implements Serializable {

    @NotBlank(message = "userCode不能为空")
    @Schema(description = "用户编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userCode;

    @Schema(description = "用户名（显示名，用于JWT）")
    private String username;

    @NotBlank(message = "authIdentifier不能为空")
    @Schema(description = "认证标识（登录标识，用于UserSession和登录日志）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String authIdentifier;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @NotBlank(message = "authType不能为空")
    @Schema(description = "认证类型（PASSWORD/LDAP等）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String authType;

}
