package com.wkclz.iam.admin.mapper;

import com.wkclz.mybatis.mapper.BaseMapper;
import com.wkclz.iam.common.entity.IamRequestLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_request_log (系统请求日志) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamRequestLogMapper extends BaseMapper<IamRequestLog> {

    // 示例查询,可删除
    Long example();

}

