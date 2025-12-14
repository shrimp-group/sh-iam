package com.wkclz.iam.common.entity;

import com.wkclz.core.base.BaseEntity;
import com.wkclz.core.annotation.Desc;
import lombok.Data;

@Data
public class IamLoginLog extends BaseEntity {
    
    @Desc("用户编码")
    private String userCode;
    
    @Desc("登录用户名")
    private String username;
    
    @Desc("登录类型：PASSWORD(密码登录)、LDAP(LDAP登录)等")
    private String loginType;
    
    @Desc("登录状态：SUCCESS(成功)、FAILED(失败)")
    private Integer loginStatus;
    
    @Desc("登录结果消息")
    private String message;
    
    @Desc("登录IP地址")
    private String ipAddress;
    
    @Desc("用户代理信息")
    private String userAgent;
}
