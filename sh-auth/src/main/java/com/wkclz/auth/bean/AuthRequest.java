package com.wkclz.auth.bean;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

/** 认证请求 */
@Data
public class AuthRequest implements Serializable {





    private String authType;
    private Credential credential;
    private Map<String, Object> extra;
}
