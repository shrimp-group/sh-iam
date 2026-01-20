package com.wkclz.iam.admin.service;

import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.mapper.IamRoleMapper;
import com.wkclz.iam.common.entity.IamRole;
import com.wkclz.mybatis.service.BaseService;
import com.wkclz.redis.helper.RedisIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_role (角色) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_role 的逻辑. 其他逻辑放 custom 中
 */
 
@Service
public class IamRoleService extends BaseService<IamRole, IamRoleMapper> {


    @Autowired
    private RedisIdGenerator redisIdGenerator;

    public IamRole create(IamRole entity) {
        duplicateCheck(entity);
        entity.setRoleCode(redisIdGenerator.generateIdWithPrefix("role_"));
        mapper.insert(entity);
        return entity;
    }

    public IamRole update(IamRole entity) {
        duplicateCheck(entity);
        IamRole oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        IamRole.copyIfNotNull(entity, oldEntity);
        updateByIdSelective(oldEntity);
        return oldEntity;
    }

    public IamRole save(IamRole entity) {
        return entity.getId() == null ? create(entity) : update(entity);
    }

    public IamRole remove(IamRole entity) {
        IamRole oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        deleteById(oldEntity);
        return oldEntity;
    }

    private void duplicateCheck(IamRole entity) {
        // 唯一条件为空，直接通过
        if (StringUtils.isBlank(entity.getRoleCode())) {
            return;
        }
        
        // 唯一条件不为空，请设置唯一条件
        IamRole param = new IamRole();
        // 唯一条件：roleCode + appCode
        param.setRoleCode(entity.getRoleCode());
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

