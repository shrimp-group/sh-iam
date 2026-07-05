package com.wkclz.iam.admin.mapper;

import com.wkclz.iam.admin.bean.resp.MenuFieldResp;
import com.wkclz.iam.common.entity.IamMenuField;
import com.wkclz.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author shrimp
 * @table iam_menu_field (菜单字段关系) DAO 接口
 */
@Mapper
public interface IamMenuFieldMapper extends BaseMapper<IamMenuField> {

    /**
     * 根据字段编码删除关联记录（逻辑删除）
     *
     * @param fieldCode 字段编码
     * @return 删除记录数
     */
    int deleteByFieldCode(@Param("fieldCode") String fieldCode);

    /**
     * 根据菜单编码删除关联记录（逻辑删除）
     *
     * @param menuCode 菜单编码
     * @return 删除记录数
     */
    int deleteByMenuCode(@Param("menuCode") String menuCode);

    /**
     * 查询字段组已绑定的字段列表（含字段详情和API信息）
     */
    List<MenuFieldResp> listByMenuCode(@Param("menuCode") String menuCode);

}
