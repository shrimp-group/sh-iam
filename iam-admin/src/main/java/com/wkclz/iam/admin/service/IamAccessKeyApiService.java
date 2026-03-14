package com.wkclz.iam.admin.service;

import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.mapper.IamAccessKeyApiMapper;
import com.wkclz.iam.common.entity.IamAccessKeyApi;
import com.wkclz.mybatis.service.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_access_key_api (AK 接口) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_access_key_api 的逻辑. 其他逻辑放 custom 中
 */
 
@Service
public class IamAccessKeyApiService extends BaseService<IamAccessKeyApi, IamAccessKeyApiMapper> {


    public List<IamAccessKeyApi> getAccessKeyList(String appId) {
        IamAccessKeyApi param = new IamAccessKeyApi();
        param.setAppId(appId);
        return selectByEntity(param);
    }


    public IamAccessKeyApi create(IamAccessKeyApi entity) {
        duplicateCheck(entity);
        mapper.insert(entity);
        return entity;
    }

    public IamAccessKeyApi remove(IamAccessKeyApi entity) {
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        IamAccessKeyApi oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        deleteById(oldEntity);
        return oldEntity;
    }

    private void duplicateCheck(IamAccessKeyApi entity) {
        // 唯一条件为空，直接通过

        // 唯一条件不为空，请设置唯一条件
        IamAccessKeyApi param = new IamAccessKeyApi();
        param.setAppCode(entity.getAppCode());
        param.setAppId(entity.getAppId());
        param.setApiCode(entity.getApiCode());
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

