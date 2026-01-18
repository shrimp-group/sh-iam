package com.wkclz.iam.common.entity;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;




/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_app (应用) 重新生成代码会覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamApp extends BaseEntity {

    /**
     * 应用编码
     */
    @Desc("应用编码")
    private String appCode;

    /**
     * 应用名称
     */
    @Desc("应用名称")
    private String appName;

    /**
     * 应用域名
     */
    @Desc("应用域名")
    private String domain;

    /**
     * 鉴权类型
     */
    @Desc("鉴权类型")
    private String authType;

    /**
     * 图标
     */
    @Desc("图标")
    private String appIcon;

    /**
     * 登录页背景
     */
    @Desc("登录页背景")
    private String loginBgp;


    public static IamApp copy(IamApp source, IamApp target) {
        if (target == null ) { target = new IamApp();}
        if (source == null) { return target; }
        target.setId(source.getId());
        target.setAppCode(source.getAppCode());
        target.setAppName(source.getAppName());
        target.setDomain(source.getDomain());
        target.setAuthType(source.getAuthType());
        target.setAppIcon(source.getAppIcon());
        target.setLoginBgp(source.getLoginBgp());
        target.setSort(source.getSort());
        target.setCreateTime(source.getCreateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        target.setVersion(source.getVersion());
        return target;
    }

    public static IamApp copyIfNotNull(IamApp source, IamApp target) {
        if (target == null ) { target = new IamApp();}
        if (source == null) { return target; }
        if (source.getId() != null) { target.setId(source.getId()); }
        if (source.getAppCode() != null) { target.setAppCode(source.getAppCode()); }
        if (source.getAppName() != null) { target.setAppName(source.getAppName()); }
        if (source.getDomain() != null) { target.setDomain(source.getDomain()); }
        if (source.getAuthType() != null) { target.setAuthType(source.getAuthType()); }
        if (source.getAppIcon() != null) { target.setAppIcon(source.getAppIcon()); }
        if (source.getLoginBgp() != null) { target.setLoginBgp(source.getLoginBgp()); }
        if (source.getSort() != null) { target.setSort(source.getSort()); }
        if (source.getCreateTime() != null) { target.setCreateTime(source.getCreateTime()); }
        if (source.getCreateBy() != null) { target.setCreateBy(source.getCreateBy()); }
        if (source.getUpdateTime() != null) { target.setUpdateTime(source.getUpdateTime()); }
        if (source.getUpdateBy() != null) { target.setUpdateBy(source.getUpdateBy()); }
        if (source.getRemark() != null) { target.setRemark(source.getRemark()); }
        if (source.getVersion() != null) { target.setVersion(source.getVersion()); }
        return target;
    }

}

