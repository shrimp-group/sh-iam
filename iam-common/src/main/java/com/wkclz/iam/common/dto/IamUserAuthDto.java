package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamUserAuth;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table IamUserAuth () 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
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



    /**
     * entity 转 Dto
     * @param source
     * @return
     */
    public static IamUserAuthDto copy(IamUserAuth source) {
        IamUserAuthDto target = new IamUserAuthDto();
        IamUserAuth.copy(source, target);
        return target;
    }
}

