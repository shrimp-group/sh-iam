package com.wkclz.iam.admin.service;

import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.mapper.IamUserAuthPasswordMapper;
import com.wkclz.iam.common.entity.IamUserAuthPassword;
import com.wkclz.mybatis.service.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_user_auth_password (密码认证表) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_user_auth_password 的逻辑. 其他逻辑放 custom 中
 */
 
@Service
public class IamUserAuthPasswordService extends BaseService<IamUserAuthPassword, IamUserAuthPasswordMapper> {

    // 示例方法，可删除
    public Long example() {
        return mapper.example();
    }

    public IamUserAuthPassword create(IamUserAuthPassword entity) {
        duplicateCheck(entity);
        mapper.insert(entity);
        return entity;
    }

    public IamUserAuthPassword update(IamUserAuthPassword entity) {
        duplicateCheck(entity);
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        Assert.notNull(entity.getVersion(), "请求错误！参数[version]不能为空");
        IamUserAuthPassword oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        IamUserAuthPassword.copyIfNotNull(entity, oldEntity);
        updateByIdSelective(oldEntity);
        return oldEntity;
    }

    public IamUserAuthPassword save(IamUserAuthPassword entity) {
        return entity.getId() == null ? create(entity) : update(entity);
    }

    public IamUserAuthPassword remove(IamUserAuthPassword entity) {
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        IamUserAuthPassword oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        deleteById(oldEntity);
        return oldEntity;
    }

    private void duplicateCheck(IamUserAuthPassword entity) {
        // 唯一条件为空，直接通过
        if (true) {
            return;
        }
        
        // 唯一条件不为空，请设置唯一条件
        IamUserAuthPassword param = new IamUserAuthPassword();
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

