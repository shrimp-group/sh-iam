package com.wkclz.iam.sso.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author shrimp
 */
@Data
public class VueRouterMenu implements Serializable {

    private String path;
    private String name;
    private Boolean hidden;
    private String redirect;
    private String component;
    private Boolean alwaysShow;
    private VueRouterMeta meta;
    private List<VueRouterMenu> children;

}
