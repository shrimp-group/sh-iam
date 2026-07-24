package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamLoginRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description Create by sh-generator
 *
 * @author shrimp
 * @table IamLoginRecord (登录记录表) 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamLoginRecordDto extends IamLoginRecord {


    /**
     * entity 转 Dto
     *
     * @param source
     * @return
     */
    public static IamLoginRecordDto copy(IamLoginRecord source) {
        IamLoginRecordDto target = new IamLoginRecordDto();
        IamLoginRecord.copy(source, target);
        return target;
    }
}

