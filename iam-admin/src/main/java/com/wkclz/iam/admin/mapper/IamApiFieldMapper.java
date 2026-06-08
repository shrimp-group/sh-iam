package com.wkclz.iam.admin.mapper;

import com.wkclz.iam.common.dto.IamApiFieldDto;
import com.wkclz.iam.common.entity.IamApiField;
import com.wkclz.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author shrimp
 * @table iam_api_field (API字段权限) DAO 接口
 */
@Mapper
public interface IamApiFieldMapper extends BaseMapper<IamApiField> {

    /**
     * 分页查询API字段权限列表（关联查询API信息）
     */
    List<IamApiFieldDto> getApiFieldList(IamApiFieldDto entity);

}
