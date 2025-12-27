package com.wkclz.iam.sso.mapper;

import com.wkclz.iam.common.dto.IamUserAuthDto;
import com.wkclz.iam.common.entity.IamUserAuth;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SsoLoginMapper {


    // 用户名，获取密码方式登录的用户信息
    IamUserAuthDto getUserAuth4PasswordByUsername(@Param("username") String username);

    Integer updateUserLoginInfo(IamUserAuth entity);

}
