package com.wkclz.iam.admin.service;

import com.wkclz.iam.admin.mapper.IamMenuMapper;
import com.wkclz.iam.common.dto.IamMenuDto;
import com.wkclz.iam.common.entity.IamMenu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IamUserMenuService {

    @Autowired
    private IamMenuMapper iamMenuMapper;
    
    /**
     * 根据用户编码查询菜单列表
     * @param entity 查询条件，包含userCode
     * @return 菜单列表
     */
    public List<IamMenuDto> userMenuList(IamMenu entity) {
        // 1. 根据用户编码查询菜单列表
        List<IamMenu> menus = iamMenuMapper.selectMenusByUserCode(entity);
        // 2. 转换为DTO并返回
        return menus.stream().map(IamMenuDto::copy).collect(Collectors.toList());
    }
    
    /**
     * 根据用户编码查询菜单树
     * @param entity 查询条件，包含userCode
     * @return 菜单树
     */
    public List<IamMenuDto> userMenuTree(IamMenu entity) {
        // 1. 根据用户编码查询菜单列表
        List<IamMenu> menus = iamMenuMapper.selectMenusByUserCode(entity);
        // 2. 构建菜单树
        return buildMenuTree(menus);
    }
    
    /**
     * 构建菜单树
     * @param menus 菜单列表
     * @return 菜单树
     */
    private List<IamMenuDto> buildMenuTree(List<IamMenu> menus) {
        List<IamMenuDto> tree = new ArrayList<>();

        Map<String, IamMenuDto> menuMap = menus.stream()
                .map(IamMenuDto::copy)
                .collect(Collectors.toMap(IamMenuDto::getMenuCode, t -> t));

        for (IamMenuDto menuDto : menuMap.values()) {
            String parentCode = menuDto.getParentCode();
            // 如果是顶级菜单（父编码为"0"），直接放入tree
            if ("0".equals(parentCode)) {
                tree.add(menuDto);
            } else {
                // 否则，放入父菜单的children列表
                IamMenuDto parentNode = menuMap.get(parentCode);
                if (parentNode != null) {
                    parentNode.getChildren().add(menuDto);
                }
            }
        }
        return tree;
    }
}