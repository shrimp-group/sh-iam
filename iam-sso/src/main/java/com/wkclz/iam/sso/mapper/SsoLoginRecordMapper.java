package com.wkclz.iam.sso.mapper;

import com.wkclz.iam.common.entity.IamLoginRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SsoLoginRecordMapper {


    Integer insertLoginRecord(IamLoginRecord entity);


    /**
     * 原实现仅返回最近一条记录的 loginStatus，无法正确触发验证码保护
     */
    IamLoginRecord getLastLoginIn1Hour(IamLoginRecord entity);

}
