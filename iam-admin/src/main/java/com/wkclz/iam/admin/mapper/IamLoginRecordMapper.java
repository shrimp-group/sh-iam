package com.wkclz.iam.admin.mapper;

import com.wkclz.mybatis.mapper.BaseMapper;
import com.wkclz.iam.common.entity.IamLoginRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Description Create by sh-generator
 *
 * @author shrimp
 * @table iam_login_record (登录记录表) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamLoginRecordMapper extends BaseMapper<IamLoginRecord> {

    List<IamLoginRecord> getLoginRecordList(IamLoginRecord entity);

}

