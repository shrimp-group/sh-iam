package com.wkclz.iam.admin.mapper;

import com.wkclz.iam.admin.bean.req.RoleUserPageReq;
import com.wkclz.mybatis.mapper.BaseMapper;
import com.wkclz.iam.admin.bean.resp.*;
import com.wkclz.iam.common.dto.IamUserRoleDto;
import com.wkclz.iam.common.entity.IamUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_user_role (用户-角色关系) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamUserRoleMapper extends BaseMapper<IamUserRole> {

    /**
     * 查询用户角色列表
     */
    List<UserRoleResp> getUserRoleList(@Param("userCode") String userCode, @Param("roleCode") String roleCode);

    /**
     * 角色下用户分页查询（支持用户名精确匹配、姓名模糊搜索）
     */
    List<RoleUserResp> getRoleUserPage(RoleUserPageReq req);

    /**
     * 用户在某应用下的角色树查询（含 bindCount 标记）
     */
    List<IamUserRoleDto> getUserRoleTree(@Param("userCode") String userCode, @Param("appCode") String appCode);

    /**
     * 查询菜单绑定的角色列表
     */
    List<MenuRoleResp> getMenuBoundRoles(@Param("menuCode") String menuCode);

    /**
     * 查询菜单关联的用户列表（含来源角色）
     */
    List<MenuUserResp> getMenuBoundUsers(@Param("menuCode") String menuCode);

    /**
     * 查询用户菜单来源角色信息
     */
    List<UserMenuSourceResp> getUserMenuSourceList(@Param("userCode") String userCode, @Param("appCode") String appCode);

    /**
     * 批量启用已到有效期的绑定
     */
    int enableExpiredBindings();

    /**
     * 批量禁用已过有效期的绑定
     */
    int disableExpiredBindings();

}

