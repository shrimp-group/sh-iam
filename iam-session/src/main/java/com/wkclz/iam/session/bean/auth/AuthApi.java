package com.wkclz.iam.session.bean.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * API lite 模型（iam-session 模块自持轻量模型，不依赖 iam-common 实体），
 * 用于菜单-API 关联关系中的接口描述（权限计算与纳管判定依据）。
 *
 * @author shrimp
 */
@Data
public class AuthApi {

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
     * API URI（支持 Ant 通配）
     */
    @Schema(description = "API URI")
    private String apiUri;

    /**
     * 白名单标识：1-白名单免鉴权
     */
    @Schema(description = "白名单标识：1-白名单免鉴权")
    private Integer writeFlag;
}
