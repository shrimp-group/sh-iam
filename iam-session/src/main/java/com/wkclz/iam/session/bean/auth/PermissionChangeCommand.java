package com.wkclz.iam.session.bean.auth;

import com.wkclz.iam.session.enums.AuthChangeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 权限变更清理指令（Redis 线上格式，fastjson2 序列化）。
 *
 * <p>流转链路：iam-admin 发布 Spring 事件 → PermissionChangePublisher 双写
 * （Pub/Sub channel + 变更日志 Hash）→ 订阅/轮询接收 → AuthCacheManager.applyCommand
 * （时间戳去重，仅处理更新的同类指令）。</p>
 *
 * @author shrimp
 */
@Data
public class PermissionChangeCommand {

    /**
     * 变更类型
     */
    @Schema(description = "变更类型")
    private AuthChangeType type;

    /**
     * 目标键（APP_STATIC 时为 appCode，USER 时为 userCode）
     */
    @Schema(description = "目标键")
    private String targetKey;

    /**
     * 指令时间戳（毫秒），同 type+targetKey 仅处理最新
     */
    @Schema(description = "指令时间戳（毫秒）")
    private long timestamp;
}
