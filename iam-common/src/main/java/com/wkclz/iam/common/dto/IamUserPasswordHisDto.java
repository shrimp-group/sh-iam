package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamUserPasswordHis;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table IamUserPasswordHis () 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamUserPasswordHisDto extends IamUserPasswordHis {




    /**
     * entity 转 Dto
     * @param source
     * @return
     */
    public static IamUserPasswordHisDto copy(IamUserPasswordHis source) {
        IamUserPasswordHisDto target = new IamUserPasswordHisDto();
        IamUserPasswordHis.copy(source, target);
        return target;
    }
}

