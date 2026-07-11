package com.wkclz.auth.bean;

import com.wkclz.auth.enums.FieldPermissionType;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/** 权限计算结果缓存 */
@Data
public class ResolvedAuthorization implements Serializable {
    private String subjectId;
    private String appCode;

    /** API 权限："GET:/api/user/page" O(1) 查找 */
    private Set<String> apiPermissions;

    /** 字段权限：apiCode → (fieldName → 权限类型) */
    private Map<String, Map<String, FieldPermissionType>> fieldPermissions;

    /** 数据权限：dimensionCode → {value1, value2, ...} */
    private Map<String, Set<String>> dataScopes;

    private LocalDateTime computedTime;
}
