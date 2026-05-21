package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamTenant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Description Create by sh-generator
 *
 * @author shrimp
 * @table IamTenant (租户) 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamTenantDto extends IamTenant {


    /**
     * entity 转 Dto
     *
     * @param source
     * @return
     */
    public static IamTenantDto copy(IamTenant source) {
        IamTenantDto target = new IamTenantDto();
        IamTenant.copy(source, target);
        return target;
    }
}

