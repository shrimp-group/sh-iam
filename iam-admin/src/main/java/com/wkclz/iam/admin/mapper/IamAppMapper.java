package com.wkclz.iam.admin.mapper;

import com.wkclz.iam.common.entity.IamApp;
import com.wkclz.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_app (应用) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamAppMapper extends BaseMapper<IamApp> {

    List<IamApp> getAppList(IamApp entity);

    List<IamApp> getAppOptions();


}

