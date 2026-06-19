package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "接口文档请求")
public class ApiDocReq implements Serializable {

    @NotBlank(message = "请求方法不能为空")
    @Schema(description = "请求方法")
    private String method;

    @NotBlank(message = "请求URI不能为空")
    @Schema(description = "请求URI")
    private String uri;
}
