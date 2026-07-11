package com.wkclz.auth.enums;

/** 菜单类型 */
public enum MenuType {
    MENU("菜单"),
    BUTTON("按钮");

    private final String desc;
    MenuType(String desc) { this.desc = desc; }
    public String getDesc() { return desc; }
}
