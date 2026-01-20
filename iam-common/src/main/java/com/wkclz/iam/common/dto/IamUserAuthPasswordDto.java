package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamUserAuthPassword;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table IamUserAuthPassword () 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamUserAuthPasswordDto extends IamUserAuthPassword {




    /**
     * entity 转 Dto
     * @param source
     * @return
     */
    public static IamUserAuthPasswordDto copy(IamUserAuthPassword source) {
        IamUserAuthPasswordDto target = new IamUserAuthPasswordDto();
        IamUserAuthPassword.copy(source, target);
        return target;
    }
}

