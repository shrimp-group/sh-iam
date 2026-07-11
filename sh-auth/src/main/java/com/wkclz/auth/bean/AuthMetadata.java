package com.wkclz.auth.bean;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 应用级 RBAC 缓存快照（单例，所有用户共享） */
@Data
public class AuthMetadata implements Serializable {
    private String appCode;
    private Map<String, Role> roles;
    private Map<String, MenuNode> menus;
    private Map<String, ApiResource> apis;
    private Map<String, List<String>> roleMenus;
    private Map<String, List<String>> menuApis;
    private Map<String, List<ApiField>> apiFields;
    private LocalDateTime loadTime;
}
