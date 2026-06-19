package com.wkclz.iam.sdk.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录后的缓存对象，用于控制用户会话过程
 */
@Data
@Schema(description = "用户会话信息")
public class UserSession implements Serializable {

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "认证类型")
    private String authType;

    /**
     * 小程序登录场景
     */
    @Schema(description = "微信OpenID")
    private String openId;

}
