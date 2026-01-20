package com.wkclz.iam.admin.mapper;

import com.wkclz.mybatis.mapper.BaseMapper;
import com.wkclz.iam.common.entity.IamUserAuth;
import org.apache.ibatis.annotations.Mapper;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_user_auth (用户认证关系表) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamUserAuthMapper extends BaseMapper<IamUserAuth> {

    // 示例查询,可删除
    Long example();

}

