package com.wkclz.iam.common.entity;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;


/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_user_auth (用户认证关系表) 重新生成代码会覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamUserAuth extends BaseEntity {

    /**
     * 用户编码
     */
    @Desc("用户编码")
    private String userCode;

    /**
     * 认证类型：PASSWORD(密码)、LDAP(LDAP认证)、第三方认证需结合代码实现
     */
    @Desc("认证类型：PASSWORD(密码)、LDAP(LDAP认证)、第三方认证需结合代码实现")
    private String authType;

    /**
     * 认证标识：密码认证时为用户名，第三方认证时为第三方用户ID
     */
    @Desc("认证标识：密码认证时为用户名，第三方认证时为第三方用户ID")
    private String authIdentifier;

    /**
     * 最后认证时间
     */
    @Desc("最后认证时间")
    private LocalDateTime lastAuthTime;

    /**
     * 状态：0-禁用,1-启用
     */
    @Desc("状态：0-禁用,1-启用")
    private Integer authStatus;

    /**
     * 最后登录时间
     */
    @Desc("最后登录时间")
    private LocalDateTime lastLoginTime;

    /**
     * 最后登录IP
     */
    @Desc("最后登录IP")
    private String lastLoginIp;

    /**
     * 登录次数
     */
    @Desc("登录次数")
    private Integer loginCount;


    public static IamUserAuth copy(IamUserAuth source, IamUserAuth target) {
        if (target == null ) { target = new IamUserAuth();}
        if (source == null) { return target; }
        target.setId(source.getId());
        target.setUserCode(source.getUserCode());
        target.setAuthType(source.getAuthType());
        target.setAuthIdentifier(source.getAuthIdentifier());
        target.setLastAuthTime(source.getLastAuthTime());
        target.setAuthStatus(source.getAuthStatus());
        target.setLastLoginTime(source.getLastLoginTime());
        target.setLastLoginIp(source.getLastLoginIp());
        target.setLoginCount(source.getLoginCount());
        target.setSort(source.getSort());
        target.setCreateTime(source.getCreateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        target.setVersion(source.getVersion());
        return target;
    }

    public static IamUserAuth copyIfNotNull(IamUserAuth source, IamUserAuth target) {
        if (target == null ) { target = new IamUserAuth();}
        if (source == null) { return target; }
        if (source.getId() != null) { target.setId(source.getId()); }
        if (source.getUserCode() != null) { target.setUserCode(source.getUserCode()); }
        if (source.getAuthType() != null) { target.setAuthType(source.getAuthType()); }
        if (source.getAuthIdentifier() != null) { target.setAuthIdentifier(source.getAuthIdentifier()); }
        if (source.getLastAuthTime() != null) { target.setLastAuthTime(source.getLastAuthTime()); }
        if (source.getAuthStatus() != null) { target.setAuthStatus(source.getAuthStatus()); }
        if (source.getLastLoginTime() != null) { target.setLastLoginTime(source.getLastLoginTime()); }
        if (source.getLastLoginIp() != null) { target.setLastLoginIp(source.getLastLoginIp()); }
        if (source.getLoginCount() != null) { target.setLoginCount(source.getLoginCount()); }
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

