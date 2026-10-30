package com.wkclz.iam.admin.mapper;

import com.wkclz.iam.common.entity.IamApiField;
import com.wkclz.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author shrimp
 * @table iam_api_field (API字段权限) DAO 接口
 */
@Mapper
public interface IamApiFieldMapper extends BaseMapper<IamApiField> {

    /**
     * 按 API 编码列表批量查询字段权限
     */
    List<IamApiField> getFieldsByApiCodes(@Param("apiCodes") List<String> apiCodes);

    /**
     * 导入场景批量更新（逐行不同值，CASE WHEN 单 SQL）
     */
    int updateBatch4Import(@Param("entities") List<IamApiField> entities);

}
