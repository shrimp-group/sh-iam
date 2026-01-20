package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamLoginLog;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table IamLoginLog () 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamLoginLogDto extends IamLoginLog {




    /**
     * entity 转 Dto
     * @param source
     * @return
     */
    public static IamLoginLogDto copy(IamLoginLog source) {
        IamLoginLogDto target = new IamLoginLogDto();
        IamLoginLog.copy(source, target);
        return target;
    }
}

