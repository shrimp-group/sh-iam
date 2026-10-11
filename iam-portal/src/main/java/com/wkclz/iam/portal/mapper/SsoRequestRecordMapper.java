package com.wkclz.iam.portal.mapper;

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

