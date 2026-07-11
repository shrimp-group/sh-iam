package com.wkclz.auth.bean;

import lombok.Data;
import java.io.Serializable;

/** 权限定义 */
@Data
public class AuthPermission implements Serializable {
    private String resource;
    private String action;
    private String description;
}
