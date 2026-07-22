package com.wkclz.iam.session.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应。
 *
 * <p>成功时 token/userCode/username/nickname/avatar 有值，loginStatus=0。
 * 失败时 loginStatus 和 loginMessage 有值，其余字段为空。</p>
 */
@Data
@Schema(description = "登录响应")
public class LoginResp implements Serializable {

    @Schema(description = "登录 Token (JWT)")
    private String token;

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "登录状态码 (0=成功)")
    private Integer loginStatus;

    @Schema(description = "登录信息")
    private String loginMessage;
}
