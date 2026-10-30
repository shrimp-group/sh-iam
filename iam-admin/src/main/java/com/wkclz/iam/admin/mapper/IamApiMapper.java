package com.wkclz.iam.admin.mapper;

import com.wkclz.iam.common.dto.IamApiDto;
import com.wkclz.iam.common.entity.IamApi;
import com.wkclz.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_api (路由映射) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamApiMapper extends BaseMapper<IamApi> {

    List<IamApiDto> getApiList(IamApiDto entity);

    List<IamApi> getApiOptions(IamApi entity);

    List<IamApi> getApis4Copy(IamApi entity);

    List<IamApi> getApis4Paste(@Param("apiCodes") List<String> apiCodes );

    /**
     * 导入场景批量更新（逐行不同值，CASE WHEN 单 SQL）
     */
    int updateBatch4Import(@Param("entities") List<IamApi> entities);

}

