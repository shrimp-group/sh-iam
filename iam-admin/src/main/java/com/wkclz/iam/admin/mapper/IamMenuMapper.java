package com.wkclz.iam.admin.mapper;

import com.wkclz.iam.common.dto.IamMenuDto;
import com.wkclz.iam.common.entity.IamMenu;
import com.wkclz.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_menu (菜单) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamMenuMapper extends BaseMapper<IamMenu> {

    List<IamMenuDto> getAppMenuList(@Param("appCode") String appCode);

    List<IamMenuDto> getAppMenu4Tree(@Param("appCode") String appCode);

    /**
     * 按 appCode 集合批量查询菜单：导出选中模式圈定子孙所用，列与 getAppMenu4Tree 一致
     *
     * @param appCodes appCode 集合
     * @return 菜单列表
     */
    List<IamMenuDto> getMenusByAppCodes(@Param("appCodes") List<String> appCodes);

    /**
     * 根据用户编码查询菜单列表
     * @param entity 用户编码
     * @return 菜单列表
     */
    List<IamMenu> selectMenusByUserCode(IamMenu entity);

    /**
     * 按 menuCode 集合批量查询菜单：导入存在性判断与 parentCode 存在性判断共用
     *
     * @param menuCodes menuCode 集合
     * @return 已存在菜单列表（含 id/appCode/parentCode/menuCode/menuName/menuType/version）
     */
    List<IamMenu> getMenusByCodes(@Param("menuCodes") List<String> menuCodes);

    /**
     * 导入场景批量更新：CASE WHEN 逐行不同值单 SQL，不更新 menu_code；
     * update_time 由数据库自动填充，update_by 取实体值（MyBatisUpdateInterceptor 已植入）
     *
     * @param entities 待更新实体列表（id 必填）
     * @return 更新行数
     */
    int updateBatch4Import(@Param("entities") List<IamMenu> entities);

}

