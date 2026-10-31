package com.wkclz.iam.admin.mapper;

import com.wkclz.mybatis.mapper.BaseMapper;
import com.wkclz.iam.common.entity.IamDataDimension;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_data_dimension (数据权限维度) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamDataDimensionMapper extends BaseMapper<IamDataDimension> {

    List<IamDataDimension> getDataDimensionList(IamDataDimension entity);

    List<IamDataDimension> getDims4Export(IamDataDimension entity);

    List<IamDataDimension> getDimsByCodes(@Param("dimensionCodes") List<String> dimensionCodes);

    int updateBatch4Import(@Param("entities") List<IamDataDimension> entities);

}

