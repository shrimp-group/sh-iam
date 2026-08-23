package com.wkclz.iam.session.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * API 请求控制配置数据载体（iam-session 模块自有轻量模型，不依赖 iam-common 实体），
 * 由 {@link com.wkclz.iam.session.spi.ApiRequestControlProvider} SPI 提供方返回。
 *
 * @author shrimp
 */
@Data
public class ApiControlConfig {

    /**
     * 应用编码
     */
    @Schema(description = "应用编码")
    private String appCode;

    /**
     * API 编码
     */
    @Schema(description = "API 编码")
    private String apiCode;

    /**
     * HTTP 方法
     */
    @Schema(description = "HTTP 方法")
    private String apiMethod;

    /**
     * API URI（支持 Ant 通配，如 /user/info/{id}）
     */
    @Schema(description = "API URI")
    private String apiUri;

    /**
     * 请求控制配置 JSON 字符串（对应 iam_api.request_control 列）
     */
    @Schema(description = "请求控制配置 JSON")
    private String requestControl;
}
