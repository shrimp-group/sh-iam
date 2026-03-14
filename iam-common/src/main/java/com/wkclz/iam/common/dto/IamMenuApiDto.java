package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamMenuApi;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table IamMenuApi () 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamMenuApiDto extends IamMenuApi {




    /**
     * entity 转 Dto
     * @param source
     * @return
     */
    public static IamMenuApiDto copy(IamMenuApi source) {
        IamMenuApiDto target = new IamMenuApiDto();
        IamMenuApi.copy(source, target);
        return target;
    }
}

