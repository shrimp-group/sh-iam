package com.wkclz.auth.bean;

import com.wkclz.auth.enums.FieldPermissionType;
import lombok.Data;
import java.io.Serializable;

/** 字段权限 */
@Data
public class FieldPermission implements Serializable {
    private String fieldCode;
    private String fieldName;
    private FieldPermissionType type;
    private String apiCode;

    /**
     * 是否可见（兼容 boolean 模型）
     */
    public boolean isVisible() {
        return type != null && type != FieldPermissionType.HIDDEN;
    }

    /**
     * 是否可编辑（兼容 boolean 模型）
     */
    public boolean isEditable() {
        return type == FieldPermissionType.EDITABLE;
    }
}
