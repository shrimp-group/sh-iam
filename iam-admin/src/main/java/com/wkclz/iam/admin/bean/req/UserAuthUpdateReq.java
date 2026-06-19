package com.wkclz.iam.admin.bean.req;

import com.wkclz.web.bean.UpdateReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户认证更新请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户认证更新请求")
public class UserAuthUpdateReq extends UpdateReq {

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "认证类型：PASSWORD/LDAP")
    private String authType;

    @Schema(description = "认证标识")
    private String authIdentifier;

    @Schema(description = "状态：0-禁用,1-启用")
    private Integer authStatus;

}
