package com.wkclz.auth.enums;

/** 缓存刷新范围 */
public enum RefreshScope {
    METADATA("元数据级"),
    SUBJECT("用户级"),
    ALL("全部");

    private final String desc;
    RefreshScope(String desc) { this.desc = desc; }
    public String getDesc() { return desc; }
}
