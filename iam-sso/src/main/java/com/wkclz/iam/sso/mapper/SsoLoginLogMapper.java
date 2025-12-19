package com.wkclz.iam.sso.mapper;

import com.wkclz.iam.common.entity.IamLoginLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SsoLoginLogMapper {


    Integer insertLoginLog(IamLoginLog entity);


}
