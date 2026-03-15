package com.wkclz.iam.admin.service;

import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.mapper.IamRoleDataMapper;
import com.wkclz.iam.common.entity.IamMenuApi;
import com.wkclz.iam.common.entity.IamRoleData;
import com.wkclz.mybatis.service.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_role_data (角色-数据关系) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_role_data 的逻辑. 其他逻辑放 custom 中
 */
 
@Service
public class IamRoleDataService extends BaseService<IamRoleData, IamRoleDataMapper> {



    public List<IamRoleData> getRoleDataList(String roleCode, String dimensionCode) {
        IamRoleData param = new IamRoleData();
        param.setRoleCode(roleCode);
        param.setDimensionCode(dimensionCode);
        return selectByEntity(param);
    }


    public IamRoleData create(IamRoleData entity) {
        duplicateCheck(entity);
        mapper.insert(entity);
        return entity;
    }

    public IamRoleData remove(IamRoleData entity) {
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        IamRoleData oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        deleteById(oldEntity);
        return oldEntity;
    }

    private void duplicateCheck(IamRoleData entity) {

        // 唯一条件不为空，请设置唯一条件
        IamRoleData param = new IamRoleData();
        param.setRoleCode(entity.getRoleCode());
        param.setDataCode(entity.getDataCode());
        param.setDimensionCode(entity.getDimensionCode());
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

