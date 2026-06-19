package com.wkclz.iam.sdk.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录后的JWT对象，用于给前端传递信息
 * @author shrimp
 */
@Data
@Schema(description = "用户JWT信息")
public class UserJwt implements Serializable {

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

}
