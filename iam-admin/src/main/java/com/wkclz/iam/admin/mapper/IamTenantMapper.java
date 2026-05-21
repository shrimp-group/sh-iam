package com.wkclz.iam.admin.mapper;

import com.wkclz.mybatis.mapper.BaseMapper;
import com.wkclz.iam.common.entity.IamTenant;
import org.apache.ibatis.annotations.Mapper;

/**
 * Description Create by sh-generator
 *
 * @author shrimp
 * @table iam_tenant (租户) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamTenantMapper extends BaseMapper<IamTenant> {

    // 示例查询,可删除
    Long example();

}

