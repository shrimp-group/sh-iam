package com.wkclz.iam.admin.mapper;

import com.wkclz.mybatis.mapper.BaseMapper;
import com.wkclz.iam.common.entity.IamUser;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_user (用户表) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamUserMapper extends BaseMapper<IamUser> {

    List<IamUser> getUserList(IamUser entity);


}

