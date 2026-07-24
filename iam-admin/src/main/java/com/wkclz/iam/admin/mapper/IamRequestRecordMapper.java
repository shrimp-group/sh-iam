package com.wkclz.iam.admin.mapper;

import com.wkclz.mybatis.mapper.BaseMapper;
import com.wkclz.iam.common.entity.IamRequestRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Description Create by sh-generator
 *
 * @author shrimp
 * @table iam_request_record (系统请求日志) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamRequestRecordMapper extends BaseMapper<IamRequestRecord> {

    Long getRequestRecordCount(IamRequestRecord entity);

    List<IamRequestRecord> getRequestRecordList(IamRequestRecord entity);

}

