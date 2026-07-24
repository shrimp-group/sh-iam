package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamRequestRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description Create by sh-generator
 *
 * @author shrimp
 * @table IamRequestRecord (系统请求日志) 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamRequestRecordDto extends IamRequestRecord {


    /**
     * entity 转 Dto
     *
     * @param source
     * @return
     */
    public static IamRequestRecordDto copy(IamRequestRecord source) {
        IamRequestRecordDto target = new IamRequestRecordDto();
        IamRequestRecord.copy(source, target);
        return target;
    }
}

