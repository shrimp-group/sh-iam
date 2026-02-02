package com.wkclz.iam.admin.mapper;

import com.wkclz.iam.common.entity.IamAccessKey;
import com.wkclz.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_access_key (访问密钥) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamAccessKeyMapper extends BaseMapper<IamAccessKey> {

    List<IamAccessKey> getAccessKeyList(IamAccessKey entity);

}
