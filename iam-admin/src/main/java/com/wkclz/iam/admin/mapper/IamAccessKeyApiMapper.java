package com.wkclz.iam.admin.mapper;

import com.wkclz.iam.common.entity.IamAccessKeyApi;
import com.wkclz.iam.common.entity.IamApi;
import com.wkclz.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_access_key_api (AK 接口) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamAccessKeyApiMapper extends BaseMapper<IamAccessKeyApi> {

    List<IamApi> getAkApisList(@Param("appId") String appId);

}

