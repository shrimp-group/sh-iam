package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamRoleMenu;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table IamRoleMenu () 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamRoleMenuDto extends IamRoleMenu {




    /**
     * entity 转 Dto
     * @param source
     * @return
     */
    public static IamRoleMenuDto copy(IamRoleMenu source) {
        IamRoleMenuDto target = new IamRoleMenuDto();
        IamRoleMenu.copy(source, target);
        return target;
    }
}

