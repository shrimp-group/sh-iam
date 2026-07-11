package com.wkclz.auth.bean;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 用户级授权缓存快照（每用户一份，仅角色码 + 数据权限） */
@Data
public class SubjectAuthorization implements Serializable {
    private String subjectId;
    private List<SubjectRole> roles;
    private Map<String, List<RoleDataScope>> roleDataScopes;
    private LocalDateTime loadTime;
}
