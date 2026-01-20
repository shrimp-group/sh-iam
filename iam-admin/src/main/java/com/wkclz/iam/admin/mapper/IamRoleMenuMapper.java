package com.wkclz.iam.admin.mapper;

import com.wkclz.mybatis.mapper.BaseMapper;
import com.wkclz.iam.common.entity.IamRoleMenu;
import org.apache.ibatis.annotations.Mapper;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_role_menu (角色-菜单关系) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamRoleMenuMapper extends BaseMapper<IamRoleMenu> {

    // 示例查询,可删除
    Long example();

}

