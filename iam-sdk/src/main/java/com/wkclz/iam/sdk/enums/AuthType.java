package com.wkclz.iam.sdk.enums;

public enum AuthType {

    PASSWORD("密码登录")
    ;


    private final String desc;

    AuthType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }


}
