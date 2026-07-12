package com.wkclz.auth.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 租户
 *
 * @author shrimp
 */
@Data
@Schema(description = "租户")
public class Tenant implements Serializable {

    @Schema(description = "租户编码")
    private String tenantCode;

    @Schema(description = "租户名称")
    private String tenantName;
}
