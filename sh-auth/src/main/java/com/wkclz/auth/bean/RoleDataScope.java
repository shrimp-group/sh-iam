package com.wkclz.auth.bean;

import lombok.Data;
import java.io.Serializable;

/** 角色-数据权限关联 */
@Data
public class RoleDataScope implements Serializable {
    private String roleCode;
    private String dimensionCode;
    private String scopeValue;
}
