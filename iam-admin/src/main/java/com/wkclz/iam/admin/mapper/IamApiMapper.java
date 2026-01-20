package com.wkclz.iam.admin.mapper;

import com.wkclz.iam.common.entity.IamApi;
import com.wkclz.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_api (路由映射) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamApiMapper extends BaseMapper<IamApi> {

    List<IamApi> getApiList(IamApi entity);

}

