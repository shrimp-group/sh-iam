package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamUserRole;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table IamUserRole () 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamUserRoleDto extends IamUserRole {




    /**
     * entity 转 Dto
     * @param source
     * @return
     */
    public static IamUserRoleDto copy(IamUserRole source) {
        IamUserRoleDto target = new IamUserRoleDto();
        IamUserRole.copy(source, target);
        return target;
    }
}

