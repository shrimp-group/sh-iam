package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamDataDimension;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table IamDataDimension () 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamDataDimensionDto extends IamDataDimension {




    /**
     * entity 转 Dto
     * @param source
     * @return
     */
    public static IamDataDimensionDto copy(IamDataDimension source) {
        IamDataDimensionDto target = new IamDataDimensionDto();
        IamDataDimension.copy(source, target);
        return target;
    }
}

