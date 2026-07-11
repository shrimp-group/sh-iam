package com.wkclz.iam.contract.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户会话
 * 仅保留 JWT 无法携带的动态会话数据，不与 Principal 重复字段
 *
 * @author shrimp
 */
@Data
@Schema(description = "用户会话")
public class Session implements Serializable {

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "认证类型：PASSWORD / LDAP / OAUTH 等")
    private String authType;

    @Schema(description = "认证标识符（用户名或三方平台标识）")
    private String authIdentifier;
}
