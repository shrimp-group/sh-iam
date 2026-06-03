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

    // 示例查询,可删除
    Long example();

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

}

