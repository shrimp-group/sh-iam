package com.wkclz.iam.admin.mapper;

import com.wkclz.iam.common.dto.IamRoleDto;
import com.wkclz.iam.common.entity.IamRole;
import com.wkclz.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_role (角色) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamRoleMapper extends BaseMapper<IamRole> {

    List<IamRoleDto> getAppRoleList(@Param("appCode") String appCode);

}

