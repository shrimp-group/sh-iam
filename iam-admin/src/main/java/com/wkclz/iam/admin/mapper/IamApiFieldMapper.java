package com.wkclz.iam.admin.mapper;

import com.wkclz.iam.common.entity.IamApiField;
import com.wkclz.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author shrimp
 * @table iam_api_field (API字段权限) DAO 接口
 */
@Mapper
public interface IamApiFieldMapper extends BaseMapper<IamApiField> {

}
