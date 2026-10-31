package com.wkclz.iam.admin.mapper;

import com.wkclz.iam.admin.bean.resp.ApiBoundResp;
import com.wkclz.mybatis.mapper.BaseMapper;
import com.wkclz.iam.common.entity.IamMenuApi;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_menu_api (菜单 接口) DAO 接口，代码重新生成不覆盖
 */

@Mapper
public interface IamMenuApiMapper extends BaseMapper<IamMenuApi> {

    /**
     * 查询菜单已绑定的 API 详情列表
     *
     * @param menuCode 菜单编码
     * @return API 绑定信息列表
     */
    List<ApiBoundResp> getBoundApis(@Param("menuCode") String menuCode);

    /**
     * 根据 API 编码一次性查询所有已绑定菜单的全路径
     * 使用 RECURSIVE CTE 避免 N+1 查询
     *
     * @param apiCode API编码
     * @return 菜单全路径字符串列表
     */
    List<String> getBoundMenuPathsByApiCode(@Param("apiCode") String apiCode);

    /**
     * 按 menuCode 集合批量查询绑定关系：导出挂接 apiCodes 与导入绑定快照重建共用
     *
     * @param menuCodes menuCode 集合
     * @return 绑定关系列表（appCode/menuCode/apiCode）
     */
    List<IamMenuApi> getBindingsByMenuCodes(@Param("menuCodes") List<String> menuCodes);

    /**
     * 按 menuCode 集合逻辑删除绑定关系（导入绑定快照重建先清后插）；
     * update_by 占位符由 MyBatisBoundSqlInterceptor 注入当前用户（与 BaseMapper.deleteById 同机制）
     *
     * @param menuCodes menuCode 集合
     * @return 删除行数
     */
    int deleteByMenuCodes(@Param("menuCodes") List<String> menuCodes);

}

