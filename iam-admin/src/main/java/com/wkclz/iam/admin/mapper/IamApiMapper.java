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

    List<IamApi> listAllMethodUris();

    /**
     * 按需查询：仅返回 uris 中存在的记录（仅按 api_uri IN 匹配，可走索引；method+uri 精确比对由调用方在内存完成）
     *
     * @param uris 请求 URI 集合
     * @return 已存在的记录（含 id/apiCode/apiMethod/apiUri）
     */
    List<IamApi> listByUris(@Param("uris") List<String> uris);

    int updateBatch4Import(@Param("entities") List<IamApi> entities);

}

