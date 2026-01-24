package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamAccessKey;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table IamAccessKey () 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamAccessKeyDto extends IamAccessKey {




    /**
     * entity 转 Dto
     * @param source
     * @return
     */
    public static IamAccessKeyDto copy(IamAccessKey source) {
        IamAccessKeyDto target = new IamAccessKeyDto();
        IamAccessKey.copy(source, target);
        return target;
    }
}

