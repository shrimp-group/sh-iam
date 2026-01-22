package com.wkclz.iam.sso.mapper;

import com.wkclz.iam.common.entity.IamRequestLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author wangkaicun
 */

@Mapper
public interface SsoRequestLogMapper {

    Long insertLog(IamRequestLog log);

    Integer updateMostLocation(IamRequestLog log);


}

