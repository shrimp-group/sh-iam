package com.wkclz.iam.admin.mapper;

import com.wkclz.mybatis.mapper.BaseMapper;
import com.wkclz.iam.common.entity.IamMenu;
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

    // 示例查询,可删除
    Long example();

    /**
     * 根据用户编码查询菜单列表
     * @param entity 用户编码
     * @return 菜单列表
     */
    List<IamMenu> selectMenusByUserCode(IamMenu entity);

}

