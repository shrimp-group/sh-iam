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


    /**
     * 根据用户编码查询菜单列表
     * @param entity 用户编码
     * @return 菜单列表
     */
    List<IamMenu> selectMenusByUserCode(IamMenu entity);

}

