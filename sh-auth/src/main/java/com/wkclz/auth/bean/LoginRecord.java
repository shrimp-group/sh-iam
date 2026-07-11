package com.wkclz.auth.bean;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 登录日志 */
@Data
public class LoginRecord implements Serializable {
    private String subjectId;
    private String username;
    private String authType;
    private String clientIp;
    private String userAgent;
    private Boolean success;
    private String failReason;
    private LocalDateTime loginTime;
}
