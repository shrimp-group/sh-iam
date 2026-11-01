package com.wkclz.iam.session.bean.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 接口字段权限 lite 模型（iam-session 模块自持轻量模型，不依赖 iam-common 实体），
 * 业务字段对齐 iam_api_field 表。
 *
 * @author shrimp
 */
@Data
public class AuthApiField {

    /**
     * API 编码
     */
    @Schema(description = "API 编码")
    private String apiCode;

    /**
     * 字段编码
     */
    @Schema(description = "字段编码")
    private String fieldCode;

    /**
     * 字段名称
     */
    @Schema(description = "字段名称")
    private String fieldName;

    /**
     * JSONPath 表达式
     */
    @Schema(description = "JSONPath 表达式")
    private String jsonPath;

    /**
     * 动作类型：HIDDEN / MASK / READ_ONLY
     */
    @Schema(description = "动作类型：HIDDEN / MASK / READ_ONLY")
    private String action;

    /**
     * 脱敏规则（keepHead,keepTail）
     */
    @Schema(description = "脱敏规则")
    private String maskRule;
}
