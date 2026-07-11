package com.wkclz.auth.bean;

import com.wkclz.auth.enums.AccountStatus;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 认证主体（账号实体） */
@Data
public class Subject implements Serializable {
    private String subjectId;
    private String authType;
    private String authIdentifier;
    private AccountStatus status;
    private LocalDateTime expireTime;
}
