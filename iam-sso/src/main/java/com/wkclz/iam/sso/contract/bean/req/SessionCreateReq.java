package com.wkclz.iam.sso.contract.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 会话创建请求
 * 由 SsoFacadeContract.login() 调用
 *
 * @author shrimp
 */
@Data
@Schema(description = "会话创建请求")
public class SessionCreateReq implements Serializable {

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "认证类型")
    private String authType;

    @Schema(description = "认证标识符")
    private String authIdentifier;
}
