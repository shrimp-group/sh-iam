package com.wkclz.iam.admin.service;

import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.mapper.IamApiFieldMapper;
import com.wkclz.iam.admin.mapper.IamMenuFieldMapper;
import com.wkclz.iam.admin.mapper.IamMenuMapper;
import com.wkclz.iam.admin.bean.req.MenuFieldBindReq;
import com.wkclz.iam.admin.bean.resp.MenuFieldResp;
import com.wkclz.iam.common.entity.IamApiField;
import com.wkclz.iam.common.entity.IamMenu;
import com.wkclz.iam.common.entity.IamMenuField;
import com.wkclz.mybatis.service.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author shrimp
 * @table iam_menu_field (菜单字段关系) 单表服务类
 */
@Service
public class IamMenuFieldService extends BaseService<IamMenuField, IamMenuFieldMapper> {

    private static final Logger log = LoggerFactory.getLogger(IamMenuFieldService.class);

    @Autowired
    private IamMenuMapper iamMenuMapper;
    @Autowired
    private IamApiFieldMapper iamApiFieldMapper;
    @Autowired
    private IamMenuFieldMapper iamMenuFieldMapper;

    /**
     * 查询字段组已绑定的字段列表（含字段详情和API信息）
     */
    public List<MenuFieldResp> listByMenuCode(String menuCode) {
        log.info("查询菜单字段绑定列表, menuCode={}", menuCode);
        return mapper.listByMenuCode(menuCode);
    }

    /**
     * 绑定字段到字段组
     */
    @Transactional(rollbackFor = Exception.class)
    public IamMenuField bind(MenuFieldBindReq req) {
        log.info("绑定菜单字段, menuCode={}, fieldCode={}", req.getMenuCode(), req.getFieldCode());
        // 校验菜单类型必须为 FIELDS
        validateMenuType(req.getMenuCode());

        // 唯一性校验: appCode + menuCode + fieldCode
        IamMenuField param = new IamMenuField();
        param.setAppCode(req.getAppCode());
        param.setMenuCode(req.getMenuCode());
        param.setFieldCode(req.getFieldCode());
        IamMenuField existing = selectOneByEntity(param);
        if (existing != null) {
            throw UserException.of(ResultCode.RECORD_DUPLICATE);
        }

        IamMenuField entity = new IamMenuField();
        entity.setAppCode(req.getAppCode());
        entity.setMenuCode(req.getMenuCode());
        entity.setFieldCode(req.getFieldCode());
        mapper.insert(entity);

        log.info("菜单字段绑定成功, menuCode={}, fieldCode={}", req.getMenuCode(), req.getFieldCode());
        return entity;
    }

    /**
     * 批量保存字段组的字段绑定（全量保存模式）
     */
    @Transactional(rollbackFor = Exception.class)
    public void save(String menuCode, String appCode, List<String> fieldCodes) {
        log.info("批量保存菜单字段绑定, menuCode={}, 字段数={}", menuCode, fieldCodes != null ? fieldCodes.size() : 0);

        // 校验菜单类型必须为 FIELDS
        validateMenuType(menuCode);

        // 先清除该字段组的所有绑定
        IamMenuField param = new IamMenuField();
        param.setMenuCode(menuCode);
        List<IamMenuField> existing = selectByEntity(param);
        for (IamMenuField item : existing) {
            deleteById(item);
        }

        // 批量插入新绑定
        if (fieldCodes != null && !fieldCodes.isEmpty()) {
            List<IamMenuField> list = fieldCodes.stream().map(t -> {
                IamMenuField entity = new IamMenuField();
                entity.setAppCode(appCode);
                entity.setMenuCode(menuCode);
                entity.setFieldCode(t);
                return entity;
            }).toList();
            mapper.insertBatch(list);
        }

        log.info("批量保存菜单字段绑定完成, menuCode={}", menuCode);
    }

    /**
     * 解绑字段
     */
    @Transactional(rollbackFor = Exception.class)
    public void unbind(Long id) {
        log.info("解绑菜单字段, id={}", id);
        IamMenuField entity = selectById(id);
        if (entity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        deleteById(entity);
        log.info("菜单字段解绑成功, id={}", id);
    }

    /**
     * 根据菜单编码删除所有关联记录（字段组菜单删除时调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByMenuCode(String menuCode) {
        log.info("删除菜单字段关联记录, menuCode={}", menuCode);
        IamMenuField param = new IamMenuField();
        param.setMenuCode(menuCode);
        List<IamMenuField> list = selectByEntity(param);
        for (IamMenuField item : list) {
            deleteById(item);
        }
        log.info("菜单字段关联记录删除完成, menuCode={}, 删除数={}", menuCode, list.size());
    }

    // --- 私有辅助方法 ---

    /**
     * 校验菜单类型必须为 FIELDS
     */
    private void validateMenuType(String menuCode) {
        IamMenu menuParam = new IamMenu();
        menuParam.setMenuCode(menuCode);
        IamMenu menu = iamMenuMapper.selectOneByEntity(menuParam);
        if (menu == null) {
            throw ValidationException.of("菜单不存在");
        }
        if (!"FIELDS".equals(menu.getMenuType())) {
            throw ValidationException.of("仅 FIELDS 类型的菜单支持字段绑定");
        }
    }

}
