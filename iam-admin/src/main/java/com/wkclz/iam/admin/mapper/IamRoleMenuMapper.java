package com.wkclz.iam.admin.mapper;

import com.wkclz.mybatis.mapper.BaseMapper;
import com.wkclz.iam.common.entity.IamRoleMenu;
import com.wkclz.iam.admin.bean.resp.RoleBoundResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_role_menu (角色-菜单关系) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamRoleMenuMapper extends BaseMapper<IamRoleMenu> {

    /**
     * 根据菜单编码查询已绑定的角色信息
     */
    List<RoleBoundResp> getBoundRoles(@Param("menuCode") String menuCode);

    /**
     * 根据角色编码查询已绑定的菜单编码列表
     */
    List<IamRoleMenu> getBoundMenuCodes(@Param("roleCode") String roleCode);

}

