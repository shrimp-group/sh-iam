package com.wkclz.iam.sdk.enums;

public enum LoginType {

    PASSWORD("密码登录")
    ;


    private final String desc;

    LoginType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }


}
