package com.wkclz.iam.admin.service;

import com.wkclz.core.base.PageData;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.init.RestfulScan;
import com.wkclz.iam.admin.mapper.IamApiMapper;
import com.wkclz.iam.common.entity.IamApi;
import com.wkclz.mybatis.helper.PageQuery;
import com.wkclz.mybatis.service.BaseService;
import com.wkclz.redis.helper.RedisIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_api (路由映射) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_api 的逻辑. 其他逻辑放 custom 中
 */
 
@Service
public class IamApiService extends BaseService<IamApi, IamApiMapper> {

    @Autowired
    private RestfulScan restfulScan;
    @Autowired
    private RedisIdGenerator redisIdGenerator;

    public PageData<IamApi> getApiPage(IamApi entity) {
        return PageQuery.page(entity, mapper::getApiList);
    }

    public IamApi create(IamApi entity) {
        duplicateCheck(entity);
        entity.setApiCode(redisIdGenerator.generateIdWithPrefix("api_"));
        mapper.insert(entity);
        return entity;
    }

    public IamApi update(IamApi entity) {
        duplicateCheck(entity);
        IamApi oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        IamApi.copyIfNotNull(entity, oldEntity);
        updateByIdSelective(oldEntity);
        return oldEntity;
    }

    public IamApi save(IamApi entity) {
        return entity.getId() == null ? create(entity) : update(entity);
    }

    public IamApi remove(IamApi entity) {
        IamApi oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        deleteById(oldEntity);
        return oldEntity;
    }

    public void syncApi() {
        try {
            restfulScan.run(null);
        } catch (Exception e) {
            throw new RuntimeException("API同步失败：" + e.getMessage());
        }
    }

    private void duplicateCheck(IamApi entity) {
        // 唯一条件为空，直接通过
        if (StringUtils.isBlank(entity.getApiUri()) || StringUtils.isBlank(entity.getApiMethod()) || StringUtils.isBlank(entity.getAppCode())) {
            return;
        }
        
        // 唯一条件不为空，请设置唯一条件
        IamApi param = new IamApi();
        // 唯一条件：apiUri + apiMethod + appCode
        param.setApiUri(entity.getApiUri());
        param.setApiMethod(entity.getApiMethod());
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

