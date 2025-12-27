package com.wkclz.iam.common.entity;

import com.wkclz.core.base.BaseEntity;
import com.wkclz.core.annotation.Desc;
import lombok.Data;

import java.util.Date;

@Data
public class IamUserAuth extends BaseEntity {
    
    @Desc("用户编码")
    private String userCode;
    
    @Desc("认证类型：PASSWORD(密码)、LDAP(LDAP认证)、第三方认证需结合代码实现")
    private String authType;
    
    @Desc("认证标识：密码认证时为用户名，第三方认证时为第三方用户ID")
    private String authIdentifier;
    
    @Desc("最后认证时间")
    private Date lastAuthTime;
    
    @Desc("状态：0-禁用,1-启用")
    private Integer authStatus;

    @Desc("最后登录时间")
    private Date lastLoginTime;

    @Desc("最后登录IP")
    private String lastLoginIp;

    @Desc("登录次数")
    private Integer loginCount;
}
