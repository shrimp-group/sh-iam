package com.wkclz.auth.bean;

import com.wkclz.auth.enums.FieldPermissionType;
import lombok.Data;
import java.io.Serializable;

/** API-字段权限关联 */
@Data
public class ApiField implements Serializable {
    private String apiCode;
    private String fieldName;
    private FieldPermissionType type;
}
