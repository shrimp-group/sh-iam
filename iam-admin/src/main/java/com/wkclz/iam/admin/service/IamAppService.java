package com.wkclz.iam.admin.service;

import com.wkclz.core.base.PageData;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.mapper.IamAppMapper;
import com.wkclz.iam.common.entity.IamApp;
import com.wkclz.iam.session.event.PermissionChangeEvent;
import com.wkclz.mybatis.helper.PageQuery;
import com.wkclz.mybatis.service.BaseService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_app (应用) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_app 的逻辑. 其他逻辑放 custom 中
 */

@Service
public class IamAppService extends BaseService<IamApp, IamAppMapper> {

    private static final Logger log = LoggerFactory.getLogger(IamAppService.class);

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public PageData<IamApp> getAppPage(IamApp entity) {
        return PageQuery.page(entity, mapper::getAppList);
    }

    @Transactional(rollbackFor = Exception.class)
    public IamApp create(IamApp entity) {
        duplicateCheck(entity);
        mapper.insert(entity);
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public IamApp update(IamApp entity) {
        duplicateCheck(entity);
        IamApp oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        IamApp.copyIfNotNull(entity, oldEntity);
        updateByIdSelective(oldEntity);
        publishAppChanged(oldEntity.getAppCode());
        return oldEntity;
    }

    @Transactional(rollbackFor = Exception.class)
    public IamApp save(IamApp entity) {
        return entity.getId() == null ? create(entity) : update(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public IamApp remove(IamApp entity) {
        IamApp oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        deleteById(oldEntity);
        publishAppChanged(oldEntity.getAppCode());
        return oldEntity;
    }


    public List<IamApp> getAppOptions() {
        return mapper.getAppOptions();
    }


    /**
     * 发布应用静态资源变更事件（appCode 为空跳过）
     */
    private void publishAppChanged(String appCode) {
        if (appCode == null || appCode.isEmpty()) {
            log.warn("appCode 为空，跳过权限变更发布");
            return;
        }
        eventPublisher.publishEvent(PermissionChangeEvent.appChanged(appCode));
        log.info("已发布权限变更事件: type=APP_STATIC, key={}", appCode);
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

