package com.wkclz.auth.bean;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 用户-角色关联 */
@Data
public class SubjectRole implements Serializable {
    private String subjectId;
    private String roleCode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean enableStatus;
}
