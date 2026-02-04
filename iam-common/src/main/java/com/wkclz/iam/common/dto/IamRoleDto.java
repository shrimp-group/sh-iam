package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamRole;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table IamRole () 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamRoleDto extends IamRole {


    private String childrenCount;

    private List<IamRoleDto> children;



    /**
     * entity 转 Dto
     * @param source
     * @return
     */
    public static IamRoleDto copy(IamRole source) {
        IamRoleDto target = new IamRoleDto();
        IamRole.copy(source, target);
        return target;
    }
}

