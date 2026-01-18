package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamRequestLog;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table IamRequestLog () 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamRequestLogDto extends IamRequestLog {




    /**
     * entity 转 Dto
     * @param source
     * @return
     */
    public static IamRequestLogDto copy(IamRequestLog source) {
        IamRequestLogDto target = new IamRequestLogDto();
        IamRequestLog.copy(source, target);
        return target;
    }
}

