package com.wkclz.iam.sdk.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "登录响应")
public class LoginResp implements Serializable {

    /**
     * 登录状态
     */
    @Schema(description = "登录状态")
    private Integer loginStatus;

    /**
     * 登录信息
     */
    @Schema(description = "登录信息")
    private String loginMessage;

    /**
     * 登录token, jwt
     */
    @Schema(description = "登录token")
    private String token;

}
