package com.wkclz.iam.session.enums;

/**
 * 权限缓存资源类型枚举 — 三级缓存状态映射的类型基础。
 *
 * <p>条目状态流转：NOT_LOADED →(按需加载) LOADED →(变更指令/会话终结/TTL/LRU) EVICTED(=NOT_LOADED)，
 * 由 Guava 存在性编码，状态变迁经统一 {@code [auth-cache]} 日志输出。权威状态映射表见
 * {@code com.wkclz.iam.session.cache.AuthCacheManager} 类文档。</p>
 */
public enum AuthResourceType {

    /**
     * 应用静态资源（L1）：键 appCode；按需加载，变更指令精确清理 + 容量 LRU，无 TTL（远程形态例外）
     */
    APP_STATIC("应用静态资源"),

    /**
     * 用户角色（L2）：键 userCode；按需加载，变更指令 + 会话终结 + 空闲 TTL + 容量 LRU
     */
    USER_ROLE("用户角色"),

    /**
     * 用户计算结果（L3）：键 appCode→userCode→urlKey；按 URL 按需计算，
     * URL 级 TTL + 用户级空闲 TTL + 变更指令/会话终结联动清理 + 容量 LRU
     */
    USER_COMPUTED("用户计算结果");

    private final String desc;

    AuthResourceType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

}
