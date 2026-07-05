package com.wkclz.iam.sdk.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "登出请求")
public class LogoutReq {

    @Schema(description = "用户令牌")
    private String token;

}
