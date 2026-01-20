package com.wkclz.iam.admin.service;

import com.wkclz.core.base.PageData;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.mapper.IamAppMapper;
import com.wkclz.iam.common.entity.IamApp;
import com.wkclz.mybatis.helper.PageQuery;
import com.wkclz.mybatis.service.BaseService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_app (应用) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_app 的逻辑. 其他逻辑放 custom 中
 */
 
@Service
public class IamAppService extends BaseService<IamApp, IamAppMapper> {

    public PageData<IamApp> getAppPage(IamApp entity) {
        return PageQuery.page(entity, mapper::getAppList);
    }

    public IamApp create(IamApp entity) {
        duplicateCheck(entity);
        mapper.insert(entity);
        return entity;
    }

    public IamApp update(IamApp entity) {
        duplicateCheck(entity);
        IamApp oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        IamApp.copyIfNotNull(entity, oldEntity);
        updateByIdSelective(oldEntity);
        return oldEntity;
    }

    public IamApp save(IamApp entity) {
        return entity.getId() == null ? create(entity) : update(entity);
    }

    public IamApp remove(IamApp entity) {
        IamApp oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        deleteById(oldEntity);
        return oldEntity;
    }

    private void duplicateCheck(IamApp entity) {
        // 唯一条件为空，直接通过
        if (StringUtils.isBlank(entity.getAppCode())) {
            return;
        }
        
        // 唯一条件不为空，请设置唯一条件
        IamApp param = new IamApp();
        // 唯一条件
        param.setAppCode(entity.getAppCode());
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

}

