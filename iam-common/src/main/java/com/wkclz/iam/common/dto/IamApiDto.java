package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamApi;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table IamApi () 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamApiDto extends IamApi {




    /**
     * entity 转 Dto
     * @param source
     * @return
     */
    public static IamApiDto copy(IamApi source) {
        IamApiDto target = new IamApiDto();
        IamApi.copy(source, target);
        return target;
    }
}

