package com.wkclz.auth.bean;

import com.wkclz.auth.enums.FieldPermissionType;
import lombok.Data;
import java.io.Serializable;

/** 字段权限 */
@Data
public class FieldPermission implements Serializable {
    private String fieldName;
    private FieldPermissionType type;
    private String apiCode;
}
