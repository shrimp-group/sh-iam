package com.wkclz.iam.sso.mapper;

import com.wkclz.iam.common.dto.IamUserAuthDto;
import com.wkclz.iam.common.entity.IamUser;
import com.wkclz.iam.common.entity.IamUserAuth;
import com.wkclz.iam.common.entity.IamUserAuthPassword;
import com.wkclz.iam.common.entity.IamUserPasswordHis;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SsoLoginMapper {


    // 用户名，获取密码方式登录的用户信息
    IamUserAuthDto getUserAuth4PasswordByUsername(@Param("username") String username);

    Integer updateUserLoginInfo(IamUserAuth entity);

    void updateUserLoginInfoByUserCode(@Param("userCode") String userCode,
                                       @Param("lastLoginIp") String lastLoginIp);

    List<IamUser> batchGetNicknamesByUserCodes(@Param("userCodes") List<String> userCodes);

    IamUserAuthPassword getPasswordByUserCode(@Param("userCode") String userCode);

    List<IamUserPasswordHis> getPasswordHisByUserCode(
            @Param("userCode") String userCode,
            @Param("limit") Integer limit
    );

    void updatePasswordByUserCode(IamUserAuthPassword password);

    void insertPasswordHis(IamUserPasswordHis his);

}
