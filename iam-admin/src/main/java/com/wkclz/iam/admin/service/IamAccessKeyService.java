package com.wkclz.iam.admin.service;

import com.wkclz.core.base.PageData;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.mapper.IamAccessKeyMapper;
import com.wkclz.iam.common.entity.IamAccessKey;
import com.wkclz.mybatis.helper.PageQuery;
import com.wkclz.mybatis.service.BaseService;
import com.wkclz.redis.helper.RedisIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_access_key (AK 密钥) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_access_key 的逻辑. 其他逻辑放 custom 中
 */
 
@Service
public class IamAccessKeyService extends BaseService<IamAccessKey, IamAccessKeyMapper> {

    @Autowired
    private RedisIdGenerator redisIdGenerator;

    public PageData<IamAccessKey> getAccessKeyPage(IamAccessKey entity) {
        return PageQuery.page(entity, mapper::getAccessKeyList);
    }


    public IamAccessKey create(IamAccessKey entity) {
        duplicateCheck(entity);

        String akid = redisIdGenerator.generateIdWithPrefix("ak_");
        entity.setAccessKeyId(akid);


        mapper.insert(entity);
        return entity;
    }

    public IamAccessKey update(IamAccessKey entity) {
        duplicateCheck(entity);
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        Assert.notNull(entity.getVersion(), "请求错误！参数[version]不能为空");
        IamAccessKey oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        IamAccessKey.copyIfNotNull(entity, oldEntity);
        updateByIdSelective(oldEntity);
        return oldEntity;
    }

    public IamAccessKey save(IamAccessKey entity) {
        return entity.getId() == null ? create(entity) : update(entity);
    }

    public IamAccessKey remove(IamAccessKey entity) {
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        IamAccessKey oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        deleteById(oldEntity);
        return oldEntity;
    }


    private void duplicateCheck(IamAccessKey entity) {
        // 唯一条件为空，直接通过
        if (true) {
            return;
        }
        
        // 唯一条件不为空，请设置唯一条件
        IamAccessKey param = new IamAccessKey();
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

