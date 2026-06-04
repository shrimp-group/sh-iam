package com.wkclz.iam.admin.service;

import com.wkclz.core.base.DbColumnEntity;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.bean.resp.RoleBoundResp;
import com.wkclz.iam.admin.mapper.IamRoleMenuMapper;
import com.wkclz.iam.common.entity.IamRole;
import com.wkclz.iam.common.entity.IamRoleMenu;
import com.wkclz.mybatis.service.BaseService;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_role_menu (角色-菜单关系) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_role_menu 的逻辑. 其他逻辑放 custom 中
 */
 
@Service
public class IamRoleMenuService extends BaseService<IamRoleMenu, IamRoleMenuMapper> {

    private static final Logger log = LoggerFactory.getLogger(IamRoleMenuService.class);

    @Autowired
    private IamRoleService iamRoleService;

    public IamRoleMenu create(IamRoleMenu entity) {
        duplicateCheck(entity);
        mapper.insert(entity);
        return entity;
    }

    public IamRoleMenu update(IamRoleMenu entity) {
        duplicateCheck(entity);
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        Assert.notNull(entity.getVersion(), "请求错误！参数[version]不能为空");
        IamRoleMenu oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        IamRoleMenu.copyIfNotNull(entity, oldEntity);
        updateByIdSelective(oldEntity);
        return oldEntity;
    }

    public IamRoleMenu save(IamRoleMenu entity) {
        return entity.getId() == null ? create(entity) : update(entity);
    }

    public IamRoleMenu remove(IamRoleMenu entity) {
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        IamRoleMenu oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        deleteById(oldEntity);
        return oldEntity;
    }

    private void duplicateCheck(IamRoleMenu entity) {
        IamRoleMenu param = new IamRoleMenu();
        param.setRoleCode(entity.getRoleCode());
        param.setMenuCode(entity.getMenuCode());
        param = selectOneByEntity(param);
        if (param == null) {
            return;
        }
        if (param.getId().equals(entity.getId())) {
            return;
        }
        throw UserException.of(ResultCode.RECORD_DUPLICATE);
    }

    /**
     * 查询角色已绑定的菜单编码列表
     */
    public List<String> getBoundMenuCodes(String roleCode) {
        log.info("查询角色已绑定菜单编码列表, roleCode={}", roleCode);
        List<IamRoleMenu> rms = mapper.getBoundMenuCodes(roleCode);
        return rms.stream().map(IamRoleMenu::getMenuCode).toList();
    }

    /**
     * 查询菜单已绑定的角色列表
     */
    public List<RoleBoundResp> getBoundRoles(String menuCode) {
        log.info("查询菜单已绑定角色列表, menuCode={}", menuCode);
        return mapper.getBoundRoles(menuCode);
    }

    /**
     * 批量保存角色-菜单绑定关系（diff 计算，最少数据库操作）
     *
     * @param roleCode  角色编码
     * @param menuCodes 需要绑定的菜单编码列表
     */
    public void saveRoleMenus(String roleCode, List<String> menuCodes) {
        log.info("批量保存角色-菜单绑定, roleCode={}, menuCodes={}", roleCode, menuCodes);

        if (menuCodes == null) {
            menuCodes = Collections.emptyList();
        }

        // 查询角色信息获取 appCode
        IamRole roleParam = new IamRole();
        roleParam.setRoleCode(roleCode);
        IamRole role = iamRoleService.selectOneByEntity(roleParam);
        String appCode = role != null ? role.getAppCode() : null;

        // 查询当前已绑定的 menuCode 列表
        List<IamRoleMenu> oldMenus = mapper.getBoundMenuCodes(roleCode);
        Set<String> oldCodeSet = oldMenus.stream().map(IamRoleMenu::getMenuCode).collect(Collectors.toSet());
        Set<String> newCodeSet = new HashSet<>(menuCodes);

        // 计算新增列表
        List<String> toAdd = menuCodes.stream().filter(t -> !oldCodeSet.contains(t)).toList();

        // 计算移除列表
        List<IamRoleMenu> toRemove = oldMenus.stream().filter(t -> !newCodeSet.contains(t.getMenuCode())).toList();

        // 新增绑定
        if (CollectionUtils.isNotEmpty(toAdd)) {
            List<IamRoleMenu> list = toAdd.stream().map(t -> {
                IamRoleMenu entity = new IamRoleMenu();
                entity.setAppCode(appCode);
                entity.setRoleCode(roleCode);
                entity.setMenuCode(t);
                return entity;
            }).toList();
            mapper.insertBatch(list);
        }

        // 移除绑定（逻辑删除）
        if (CollectionUtils.isNotEmpty(toRemove)) {
            List<Long> ids = toRemove.stream().map(DbColumnEntity::getId).toList();
            deleteByIds(ids);
        }
        log.info("角色-菜单绑定保存完成, roleCode={}, 新增{}条, 移除{}条", roleCode, toAdd.size(), toRemove.size());
    }

}

