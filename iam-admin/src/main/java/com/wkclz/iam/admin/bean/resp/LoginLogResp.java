package com.wkclz.iam.admin.bean.resp;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 登录日志响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "登录日志响应")
public class LoginLogResp extends EntityResp {

    @Schema(description = "认证标识")
    private String authIdentifier;

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "登录用户名")
    private String username;

    @Schema(description = "登录类型：PASSWORD(密码登录)、LDAP(LDAP登录)等")
    private String authType;

    @Schema(description = "登录状态：SUCCESS(成功)、FAILED(失败)")
    private Integer loginStatus;

    @Schema(description = "登录结果消息")
    private String message;

    @Schema(description = "登录IP地址")
    private String ipAddress;

    @Schema(description = "用户代理信息")
    private String userAgent;

}
