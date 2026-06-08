package com.wkclz.iam.admin.service;

import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.bean.resp.MenuDetailResp;
import com.wkclz.iam.admin.mapper.IamMenuMapper;
import com.wkclz.iam.common.dto.IamMenuDto;
import com.wkclz.iam.common.entity.IamMenu;
import com.wkclz.mybatis.service.BaseService;
import com.wkclz.redis.helper.RedisIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_menu (菜单) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_menu 的逻辑. 其他逻辑放 custom 中
 */
 
@Service
public class IamMenuService extends BaseService<IamMenu, IamMenuMapper> {

    private static final Logger log = LoggerFactory.getLogger(IamMenuService.class);

    @Autowired
    private RedisIdGenerator redisIdGenerator;

    @Autowired
    private IamMenuFieldService iamMenuFieldService;

    public List<IamMenuDto> menuList(IamMenu entity) {
        return mapper.getAppMenuList(entity.getAppCode());
    }


    public List<IamMenuDto> menuTree(IamMenu entity) {
        // 查询所有菜单
        List<IamMenuDto> menus = mapper.getAppMenu4Tree(entity.getAppCode());
        // 构建菜单树
        return buildMenuTree(menus);
    }

    public IamMenu create(IamMenu entity) {
        // FIELDS 类型校验：parentCode 必须指向 MENU 类型
        if ("FIELDS".equals(entity.getMenuType()) && !"0".equals(entity.getParentCode())) {
            IamMenu parentParam = new IamMenu();
            parentParam.setMenuCode(entity.getParentCode());
            IamMenu parentMenu = selectOneByEntity(parentParam);
            if (parentMenu == null || !"MENU".equals(parentMenu.getMenuType())) {
                throw ValidationException.of("FIELDS 类型菜单的父级必须是 MENU 类型");
            }
        }
        duplicateCheck(entity);
        entity.setMenuCode(redisIdGenerator.generateIdWithPrefix("menu_"));
        mapper.insert(entity);
        log.info("菜单创建成功, menuCode={}, menuType={}", entity.getMenuCode(), entity.getMenuType());
        return entity;
    }

    public IamMenu update(IamMenu entity) {
        duplicateCheck(entity);
        IamMenu oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        IamMenu.copyIfNotNull(entity, oldEntity);
        updateByIdSelective(oldEntity);
        return oldEntity;
    }

    public IamMenu save(IamMenu entity) {
        return entity.getId() == null ? create(entity) : update(entity);
    }

    public IamMenu remove(IamMenu entity) {
        IamMenu oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        // 检查菜单下是否有子菜单
        IamMenu param = new IamMenu();
        param.setParentCode(oldEntity.getMenuCode());
        long childrenMenuCount = mapper.selectCountByEntity(param);
        if (childrenMenuCount > 0) {
            throw ValidationException.of("请先删除子菜单");
        }
        // 如果删除的是 FIELDS 类型菜单，清理字段绑定
        if ("FIELDS".equals(oldEntity.getMenuType())) {
            iamMenuFieldService.deleteByMenuCode(oldEntity.getMenuCode());
            log.info("已清理字段组菜单的字段绑定, menuCode={}", oldEntity.getMenuCode());
        }
        deleteById(oldEntity);
        return oldEntity;
    }

    /**
     * 查询菜单详情
     *
     * @param id 菜单主键ID
     * @return 菜单详情响应
     */
    public MenuDetailResp getMenuDetail(Long id) {
        log.info("查询菜单详情, id={}", id);
        IamMenu menu = selectById(id);
        if (menu == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        MenuDetailResp resp = new MenuDetailResp();
        BeanUtils.copyProperties(menu, resp);
        return resp;
    }

    private void duplicateCheck(IamMenu entity) {
        // 唯一条件为空，直接通过
        if (StringUtils.isBlank(entity.getMenuCode())) {
            return;
        }
        
        // 唯一条件不为空，请设置唯一条件
        IamMenu param = new IamMenu();
        // 唯一条件：menuCode + appCode
        param.setMenuCode(entity.getMenuCode());
        param = selectOneByEntity(param);
        if (param == null) {
            return;
        }
        if (param.getId().equals(entity.getId())) {
            return;
        }
        // 查到有值，为新增或 id 不一样场景，为数据重复
        throw UserException.of(ResultCode.RECORD_DUPLICATE);
    }

    private List<IamMenuDto> buildMenuTree(List<IamMenuDto> menus) {
        List<IamMenuDto> tree = new ArrayList<>();
        Map<String, IamMenuDto> menuMap = menus.stream()
                .collect(Collectors.toMap(IamMenuDto::getMenuCode, t -> t, (v1, v2) -> v1, LinkedHashMap::new));

        for (IamMenuDto menuDto : menuMap.values()) {
            String parentCode = menuDto.getParentCode();
            // 如果是顶级菜单（父编码为"0"），直接放入tree
            if ("0".equals(parentCode)) {
                tree.add(menuDto);
            } else {
                // 否则，放入父菜单的children列表
                IamMenuDto parentNode = menuMap.get(parentCode);
                if (parentNode != null) {
                    List<IamMenuDto> children = parentNode.getChildren();
                    if (children == null) {
                        children = new ArrayList<>();
                        parentNode.setChildren(children);
                    }
                    parentNode.getChildren().add(menuDto);
                }
            }
        }
        return tree;
    }

}

