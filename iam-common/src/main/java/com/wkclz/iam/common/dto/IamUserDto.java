package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamUser;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table IamUser () 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamUserDto extends IamUser {


    private String password;



    /**
     * entity 转 Dto
     * @param source
     * @return
     */
    public static IamUserDto copy(IamUser source) {
        IamUserDto target = new IamUserDto();
        IamUser.copy(source, target);
        return target;
    }
}

