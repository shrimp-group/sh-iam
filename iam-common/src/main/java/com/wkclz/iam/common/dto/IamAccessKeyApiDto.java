package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamAccessKeyApi;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table IamAccessKeyApi () 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamAccessKeyApiDto extends IamAccessKeyApi {




    /**
     * entity 转 Dto
     * @param source
     * @return
     */
    public static IamAccessKeyApiDto copy(IamAccessKeyApi source) {
        IamAccessKeyApiDto target = new IamAccessKeyApiDto();
        IamAccessKeyApi.copy(source, target);
        return target;
    }
}

