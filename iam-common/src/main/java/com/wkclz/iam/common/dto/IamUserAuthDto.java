package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamUserAuth;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IamUserAuthDto extends IamUserAuth {

    // 密码方式登录的返回值
    private String username;
    private String nickname;
    private String phone;
    private String avatar;
    private String password;
    private String salt;
    private Integer userStatus;
    private LocalDateTime lastChangedTime;

}
