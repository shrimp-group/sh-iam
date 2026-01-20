package com.wkclz.iam.admin.mapper;

import com.wkclz.mybatis.mapper.BaseMapper;
import com.wkclz.iam.common.entity.IamUserAuthPassword;
import org.apache.ibatis.annotations.Mapper;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_user_auth_password (密码认证表) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamUserAuthPasswordMapper extends BaseMapper<IamUserAuthPassword> {

    // 示例查询,可删除
    Long example();

}

