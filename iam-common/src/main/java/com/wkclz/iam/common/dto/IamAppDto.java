package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamApp;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table IamApp () 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamAppDto extends IamApp {




    /**
     * entity 转 Dto
     * @param source
     * @return
     */
    public static IamAppDto copy(IamApp source) {
        IamAppDto target = new IamAppDto();
        IamApp.copy(source, target);
        return target;
    }
}

