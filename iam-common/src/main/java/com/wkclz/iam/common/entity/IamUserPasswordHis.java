package com.wkclz.iam.common.entity;

import com.wkclz.core.base.BaseEntity;
import com.wkclz.core.annotation.Desc;
import lombok.Data;

@Data
public class IamUserPasswordHis extends BaseEntity {
    
    @Desc("用户编码")
    private String userCode;
    
    @Desc("加密后的密码")
    private String password;
    
    @Desc("密码盐值")
    private String salt;
}
