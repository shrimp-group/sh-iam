package com.wkclz.iam.sdk.contract.bean;

import com.wkclz.auth.bean.Principal;
import com.wkclz.auth.bean.Session;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 认证结果
 * Principal + Session 聚合，由 AuthContract.authenticate() 返回
 *
 * @author shrimp
 */
@Data
@Schema(description = "认证结果")
public class AuthResult implements Serializable {

    @Schema(description = "用户主体")
    private Principal principal;

    @Schema(description = "会话信息")
    private Session session;
}
