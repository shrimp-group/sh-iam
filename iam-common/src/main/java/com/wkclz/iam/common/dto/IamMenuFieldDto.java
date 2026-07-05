package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamMenuField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description Create by sh-generator
 *
 * @author shrimp
 * @table IamMenuField (菜单字段关系) 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamMenuFieldDto extends IamMenuField {


    /**
     * entity 转 Dto
     *
     * @param source
     * @return
     */
    public static IamMenuFieldDto copy(IamMenuField source) {
        IamMenuFieldDto target = new IamMenuFieldDto();
        IamMenuField.copy(source, target);
        return target;
    }
}
