package com.wkclz.iam.session.bean.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用信息 lite 模型（对应 iam_app 表，iam-session 模块自持轻量模型，不依赖 iam-common 实体），
 * 作为 app 级信息容器，为将来更多 app 级权限控制预留扩展位。
 *
 * <p>随 {@link AppAuthResource} 经 L1 缓存加载，缓存内实例为多请求共享，消费方不得修改其字段。</p>
 *
 * @author shrimp
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AuthApp {

    /**
     * 应用编码
     */
    @Schema(description = "应用编码")
    private String appCode;

    /**
     * 应用名称
     */
    @Schema(description = "应用名称")
    private String appName;

    /**
     * 鉴权类型：TOKEN-仅会话认证 / TOKEN_API-会话认证+接口权限
     */
    @Schema(description = "鉴权类型：TOKEN-仅会话认证 / TOKEN_API-会话认证+接口权限")
    private String authType;
}
