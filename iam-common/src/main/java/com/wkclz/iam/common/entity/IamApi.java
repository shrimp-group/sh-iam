package com.wkclz.iam.common.entity;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.base.BaseEntity;
import lombok.Data;

@Data
public class IamApi extends BaseEntity {

    /**
     * 模块
     */
    @Desc("模块")
    private String module;

    /**
     * 应用编码
     */
    @Desc("应用编码")
    private String appCode;

    /**
     * 路由映射编码
     */
    @Desc("路由映射编码")
    private String apiCode;

    /**
     * 路由映射方法
     */
    @Desc("路由映射方法")
    private String apiMethod;

    /**
     * 路由映射URI
     */
    @Desc("路由映射URI")
    private String apiUri;

    /**
     * 路由映射名称
     */
    @Desc("路由映射名称")
    private String apiName;

    /**
     * 白名单
     */
    @Desc("白名单")
    private Integer writeFlag;

}
