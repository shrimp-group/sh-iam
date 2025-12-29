package com.wkclz.iam.admin.mapper;

import com.wkclz.iam.common.entity.IamUser;
import com.wkclz.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IamUserMapper extends BaseMapper<IamUser> {

    List<IamUser> getUserList(IamUser entity);

}
