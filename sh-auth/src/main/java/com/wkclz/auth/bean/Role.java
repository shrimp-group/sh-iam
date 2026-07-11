package com.wkclz.auth.bean;

import lombok.Data;
import java.io.Serializable;

/** 角色 */
@Data
public class Role implements Serializable {
    private String roleCode;
    private String roleName;
    private String parentCode;
    private String appCode;
    private String tenantCode;
    private Boolean applicable;
}
