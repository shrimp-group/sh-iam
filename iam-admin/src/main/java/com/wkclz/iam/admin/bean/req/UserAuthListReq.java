package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户认证列表查询请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户认证列表查询请求")
public class UserAuthListReq implements Serializable {

    @Schema(description = "用户编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userCode;

    @Schema(description = "认证类型：PASSWORD/LDAP")
    private String authType;

    @Schema(description = "状态：0-禁用,1-启用")
    private Integer authStatus;

}
