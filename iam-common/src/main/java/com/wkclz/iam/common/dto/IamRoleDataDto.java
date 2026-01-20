package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamRoleData;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table IamRoleData () 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamRoleDataDto extends IamRoleData {




    /**
     * entity 转 Dto
     * @param source
     * @return
     */
    public static IamRoleDataDto copy(IamRoleData source) {
        IamRoleDataDto target = new IamRoleDataDto();
        IamRoleData.copy(source, target);
        return target;
    }
}

