package com.wkclz.auth.bean;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 会话 */
@Data
public class Session implements Serializable {
    private String sessionId;
    private String subjectId;
    private Principal principal;
    private String authType;
    private String authIdentifier;
    private LocalDateTime createTime;
    private LocalDateTime expireTime;
    private String clientIp;
    private String userAgent;
}
