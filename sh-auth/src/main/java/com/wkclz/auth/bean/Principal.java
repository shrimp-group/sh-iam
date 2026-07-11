package com.wkclz.auth.bean;

import lombok.Data;
import java.io.Serializable;

/** 用户主体 */
@Data
public class Principal implements Serializable {
    private String userCode;
    private String username;
    private String nickname;
    private String avatar;
    private String appCode;
    private String authIdentifier; // 认证标识（用户名/手机号/openId，按 authType 区分含义，可选）
}
