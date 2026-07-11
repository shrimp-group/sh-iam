package com.wkclz.auth.bean;

import com.wkclz.auth.enums.MenuType;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/** 菜单节点 */
@Data
public class MenuNode implements Serializable {
    private String menuCode;
    private String parentCode;
    private String menuName;
    private MenuType menuType;
    private String routePath;
    private String component;
    private String buttonCode;
    private String appCode;
    private String icon;
    private List<MenuNode> children;
}
