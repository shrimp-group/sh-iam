package com.wkclz.iam.portal.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "按用户加载有效角色请求")
public class AuthUserRolesReq implements Serializable {

    @NotBlank(message = "用户编码不能为空")
    @Schema(description = "用户编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userCode;

}
