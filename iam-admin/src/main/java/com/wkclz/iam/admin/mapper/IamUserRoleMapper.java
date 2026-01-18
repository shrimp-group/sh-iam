package com.wkclz.iam.admin.mapper;

import com.wkclz.mybatis.mapper.BaseMapper;
import com.wkclz.iam.common.entity.IamUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_user_role (用户-角色关系) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamUserRoleMapper extends BaseMapper<IamUserRole> {

    // 示例查询,可删除
    Long example();

}

