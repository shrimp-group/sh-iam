package com.wkclz.iam.sdk.contract.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户主体
 * 从 JWT claims 解析，包含认证后的固有属性（你是谁）
 * 不含 tenantCode（租户是运行时动态切换值，从请求头获取）
 *
 * @author shrimp
 */
@Deprecated
@Data
@Schema(description = "用户主体")
public class Principal implements Serializable {

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "三方平台标识符")
    private String authIdentifier;
}
