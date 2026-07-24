package com.wkclz.iam.sso.mapper;

import com.wkclz.iam.common.entity.IamRequestRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author wangkaicun
 */

@Mapper
public interface SsoRequestRecordMapper {

    Long insertRecord(IamRequestRecord record);

    Integer updateMostLocation(IamRequestRecord record);


}

