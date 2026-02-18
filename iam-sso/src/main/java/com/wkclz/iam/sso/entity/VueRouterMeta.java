package com.wkclz.iam.sso.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @author shrimp
 */
@Data
public class VueRouterMeta implements Serializable {

    private String title;
    private String icon;
    private Boolean noCache;
    private String link;

}
