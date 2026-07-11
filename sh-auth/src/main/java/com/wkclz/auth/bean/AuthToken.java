package com.wkclz.auth.bean;

import com.wkclz.auth.enums.TokenType;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 认证令牌 */
@Data
public class AuthToken implements Serializable {
    private TokenType type;
    private String tokenValue;
    private LocalDateTime expireTime;
    private LocalDateTime issueTime;
}
