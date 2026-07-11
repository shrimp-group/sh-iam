package com.wkclz.auth.enums;

/** 字段权限类型 */
public enum FieldPermissionType {
    READ("只读"),
    WRITE("读写"),
    HIDDEN("隐藏");

    private final String desc;
    FieldPermissionType(String desc) { this.desc = desc; }
    public String getDesc() { return desc; }
}
