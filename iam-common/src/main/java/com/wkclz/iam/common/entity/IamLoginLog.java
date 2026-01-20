package com.wkclz.iam.common.entity;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;




/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_login_log (登录记录表) 重新生成代码会覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamLoginLog extends BaseEntity {

    /**
     * 认证标识
     */
    @Desc("认证标识")
    private String authIdentifier;

    /**
     * 用户编码
     */
    @Desc("用户编码")
    private String userCode;

    /**
     * 登录用户名
     */
    @Desc("登录用户名")
    private String username;

    /**
     * 登录类型：PASSWORD(密码登录)、LDAP(LDAP登录)等
     */
    @Desc("登录类型：PASSWORD(密码登录)、LDAP(LDAP登录)等")
    private String authType;

    /**
     * 登录状态：SUCCESS(成功)、FAILED(失败)
     */
    @Desc("登录状态：SUCCESS(成功)、FAILED(失败)")
    private Integer loginStatus;

    /**
     * 登录结果消息
     */
    @Desc("登录结果消息")
    private String message;

    /**
     * 登录IP地址
     */
    @Desc("登录IP地址")
    private String ipAddress;

    /**
     * 用户代理信息
     */
    @Desc("用户代理信息")
    private String userAgent;


    public static IamLoginLog copy(IamLoginLog source, IamLoginLog target) {
        if (target == null ) { target = new IamLoginLog();}
        if (source == null) { return target; }
        target.setId(source.getId());
        target.setAuthIdentifier(source.getAuthIdentifier());
        target.setUserCode(source.getUserCode());
        target.setUsername(source.getUsername());
        target.setAuthType(source.getAuthType());
        target.setLoginStatus(source.getLoginStatus());
        target.setMessage(source.getMessage());
        target.setIpAddress(source.getIpAddress());
        target.setUserAgent(source.getUserAgent());
        target.setSort(source.getSort());
        target.setCreateTime(source.getCreateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        target.setVersion(source.getVersion());
        return target;
    }

    public static IamLoginLog copyIfNotNull(IamLoginLog source, IamLoginLog target) {
        if (target == null ) { target = new IamLoginLog();}
        if (source == null) { return target; }
        if (source.getId() != null) { target.setId(source.getId()); }
        if (source.getAuthIdentifier() != null) { target.setAuthIdentifier(source.getAuthIdentifier()); }
        if (source.getUserCode() != null) { target.setUserCode(source.getUserCode()); }
        if (source.getUsername() != null) { target.setUsername(source.getUsername()); }
        if (source.getAuthType() != null) { target.setAuthType(source.getAuthType()); }
        if (source.getLoginStatus() != null) { target.setLoginStatus(source.getLoginStatus()); }
        if (source.getMessage() != null) { target.setMessage(source.getMessage()); }
        if (source.getIpAddress() != null) { target.setIpAddress(source.getIpAddress()); }
        if (source.getUserAgent() != null) { target.setUserAgent(source.getUserAgent()); }
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

