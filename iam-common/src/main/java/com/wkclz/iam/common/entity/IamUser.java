package com.wkclz.iam.common.entity;

import com.wkclz.core.base.BaseEntity;
import com.wkclz.core.annotation.Desc;
import lombok.Data;

import java.util.Date;

@Data
public class IamUser extends BaseEntity {
    
    @Desc("用户编码")
    private String userCode;
    
    @Desc("用户名")
    private String username;
    
    @Desc("昵称")
    private String nickname;
    
    @Desc("邮箱")
    private String email;
    
    @Desc("手机号")
    private String phone;
    
    @Desc("头像")
    private String avatar;
    
    @Desc("状态：1-启用，2-禁用，3-锁定")
    private Integer userStatus;
    
    @Desc("最后登录时间")
    private Date lastLoginTime;
    
    @Desc("最后登录IP")
    private String lastLoginIp;
    
    @Desc("登录次数")
    private Integer loginCount;
}
