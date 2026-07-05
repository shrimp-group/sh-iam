package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamApiField;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description Create by sh-generator
 *
 * @author shrimp
 * @table IamApiField (API字段权限) 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamApiFieldDto extends IamApiField {

    /**
     * 请求方法（关联 iam_api）
     */
    private String apiMethod;

    /**
     * 请求URI（关联 iam_api）
     */
    private String apiUri;

    /**
     * 接口名称（关联 iam_api）
     */
    private String apiName;

    /**
     * entity 转 Dto
     *
     * @param source
     * @return
     */
    public static IamApiFieldDto copy(IamApiField source) {
        IamApiFieldDto target = new IamApiFieldDto();
        IamApiField.copy(source, target);
        return target;
    }
}
