package com.wkclz.iam.admin.service;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.wkclz.core.base.PageData;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.mapper.IamLoginRecordMapper;
import com.wkclz.iam.common.entity.IamLoginRecord;
import com.wkclz.mybatis.helper.PageQuery;
import com.wkclz.mybatis.service.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_login_record (登录记录表) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_login_record 的逻辑. 其他逻辑放 custom 中
 */

@Service
public class IamLoginRecordService extends BaseService<IamLoginRecord, IamLoginRecordMapper> {

    private static final Logger log = LoggerFactory.getLogger(IamLoginRecordService.class);

    public PageData<IamLoginRecord> getLoginRecordPage(IamLoginRecord entity) {
        // 时间间隔校验：不能超过30天
        LocalDateTime timeFrom = entity.getTimeFrom();
        LocalDateTime timeTo = entity.getTimeTo();
        if (timeFrom != null && timeTo != null) {
            long between = LocalDateTimeUtil.between(timeFrom, timeTo, ChronoUnit.DAYS);
            if (between > 30) {
                throw ValidationException.of("时间间隔不能超过30天");
            }
        }
        log.info("查询登录日志分页, timeFrom={}, timeTo={}", timeFrom, timeTo);
        return PageQuery.page(entity, mapper::getLoginRecordList);
    }

    public IamLoginRecord create(IamLoginRecord entity) {
        duplicateCheck(entity);
        mapper.insert(entity);
        return entity;
    }

    public IamLoginRecord update(IamLoginRecord entity) {
        duplicateCheck(entity);
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        Assert.notNull(entity.getVersion(), "请求错误！参数[version]不能为空");
        IamLoginRecord oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        IamLoginRecord.copyIfNotNull(entity, oldEntity);
        updateByIdSelective(oldEntity);
        return oldEntity;
    }

    public IamLoginRecord save(IamLoginRecord entity) {
        return entity.getId() == null ? create(entity) : update(entity);
    }

    public IamLoginRecord remove(IamLoginRecord entity) {
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        IamLoginRecord oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        deleteById(oldEntity);
        return oldEntity;
    }

    private void duplicateCheck(IamLoginRecord entity) {
        // 唯一条件为空，直接通过
        if (true) {
            return;
        }

        // 唯一条件不为空，请设置唯一条件
        IamLoginRecord param = new IamLoginRecord();
        // 唯一条件
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

