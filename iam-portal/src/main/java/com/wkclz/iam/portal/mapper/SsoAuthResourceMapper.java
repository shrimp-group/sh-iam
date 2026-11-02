package com.wkclz.iam.portal.mapper;

import com.wkclz.iam.common.entity.IamApi;
import com.wkclz.iam.common.entity.IamMenuApi;
import com.wkclz.iam.common.entity.IamRoleMenu;
import com.wkclz.iam.session.bean.auth.AuthApi;
import com.wkclz.iam.session.bean.auth.AuthApiField;
import com.wkclz.iam.session.bean.auth.AuthApp;
import com.wkclz.iam.session.bean.auth.AuthMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * 权限资源数据访问 — 权限缓存按需加载的唯一查询出口（LocalAuthResourceFacade 消费）。
 *
 * <p>静态资源按应用整体加载（一次加载一个应用全量），用户角色按用户加载（启用且在有效期内）。
 * 关联关系（listRoleMenus/listMenuApis）均返回纯关系实体，一对多关系由 facade 侧分组组装。</p>
 */
@Mapper
public interface SsoAuthResourceMapper {

    /**
     * 应用信息（authType 决定鉴权深度）
     */
    AuthApp selectApp(@Param("appCode") String appCode);

    /**
     * 应用内全部角色编码
     */
    Set<String> listAppRoles(@Param("appCode") String appCode);

    /**
     * 应用内全部菜单（MENU 与 BUTTON 两类）
     */
    List<AuthMenu> listAppMenus(@Param("appCode") String appCode);

    /**
     * 应用内角色菜单关系（纯关系实体，join iam_role 限定应用）
     */
    List<IamRoleMenu> listRoleMenus(@Param("appCode") String appCode);

    /**
     * 应用内全部 API 定义（apiCode → method/uri，权限匹配依据）
     */
    List<AuthApi> listAppApis(@Param("appCode") String appCode);

    /**
     * 应用内菜单-API 关联（纯关系实体，与 listRoleMenus 同构，join 限定应用与有效菜单/API）
     */
    List<IamMenuApi> listMenuApis(@Param("appCode") String appCode);

    /**
     * 应用内接口字段权限
     */
    List<AuthApiField> listApiFields(@Param("appCode") String appCode);

    /**
     * 应用内已配置请求控制的 API（request_control 非空）
     */
    List<IamApi> listRequestControlApis(@Param("appCode") String appCode);

    /**
     * 用户全部有效角色编码（启用状态且有效期覆盖当前时间，跨应用）
     */
    List<String> listUserActiveRoleCodes(@Param("userCode") String userCode);
}
