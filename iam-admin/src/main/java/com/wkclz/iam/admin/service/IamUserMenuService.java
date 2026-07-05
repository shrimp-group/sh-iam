package com.wkclz.iam.admin.service;

import com.wkclz.iam.admin.bean.resp.MenuRoleResp;
import com.wkclz.iam.admin.bean.resp.MenuUserResp;
import com.wkclz.iam.admin.bean.resp.UserMenuSourceResp;
import com.wkclz.iam.admin.mapper.IamMenuMapper;
import com.wkclz.iam.admin.mapper.IamUserRoleMapper;
import com.wkclz.iam.common.dto.IamMenuDto;
import com.wkclz.iam.common.entity.IamMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IamUserMenuService {

    private static final Logger log = LoggerFactory.getLogger(IamUserMenuService.class);

    @Autowired
    private IamMenuMapper iamMenuMapper;

    @Autowired
    private IamUserRoleMapper iamUserRoleMapper;
    
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

    /**
     * 查询用户菜单来源角色信息
     */
    public List<UserMenuSourceResp> getUserMenuSourceList(String userCode, String appCode) {
        log.info("查询用户菜单来源, userCode={}, appCode={}", userCode, appCode);
        return iamUserRoleMapper.getUserMenuSourceList(userCode, appCode);
    }

    /**
     * 查询菜单绑定的角色列表
     */
    public List<MenuRoleResp> getMenuBoundRoles(String menuCode) {
        log.info("查询菜单绑定角色, menuCode={}", menuCode);
        return iamUserRoleMapper.getMenuBoundRoles(menuCode);
    }

    /**
     * 查询菜单关联的用户列表
     */
    public List<MenuUserResp> getMenuBoundUsers(String menuCode) {
        log.info("查询菜单关联用户, menuCode={}", menuCode);
        return iamUserRoleMapper.getMenuBoundUsers(menuCode);
    }
}