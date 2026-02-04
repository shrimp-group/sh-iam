package com.wkclz.iam.admin.service;

import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.mapper.IamMenuMapper;
import com.wkclz.iam.common.dto.IamMenuDto;
import com.wkclz.iam.common.entity.IamMenu;
import com.wkclz.mybatis.service.BaseService;
import com.wkclz.redis.helper.RedisIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    @Autowired
    private RedisIdGenerator redisIdGenerator;

    public List<IamMenuDto> menuList(IamMenu entity) {
        return mapper.getAppMenuList(entity.getAppCode());
    }

    public List<IamMenuDto> menuTree(IamMenu entity) {
        // 查询所有菜单
        List<IamMenu> menus = selectByEntity(entity);
        // 构建菜单树
        return buildMenuTree(menus);
    }

    public IamMenu create(IamMenu entity) {
        duplicateCheck(entity);
        entity.setMenuCode(redisIdGenerator.generateIdWithPrefix("menu_"));
        mapper.insert(entity);
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
        deleteById(oldEntity);
        return oldEntity;
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

