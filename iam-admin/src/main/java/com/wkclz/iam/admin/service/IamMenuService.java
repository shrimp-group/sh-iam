package com.wkclz.iam.admin.service;

import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.mapper.IamMenuMapper;
import com.wkclz.iam.common.entity.IamMenu;
import com.wkclz.mybatis.service.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_menu (菜单) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_menu 的逻辑. 其他逻辑放 custom 中
 */
 
@Service
public class IamMenuService extends BaseService<IamMenu, IamMenuMapper> {

    // 示例方法，可删除
    public Long example() {
        return mapper.example();
    }

    public IamMenu create(IamMenu entity) {
        duplicateCheck(entity);
        mapper.insert(entity);
        return entity;
    }

    public IamMenu update(IamMenu entity) {
        duplicateCheck(entity);
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        Assert.notNull(entity.getVersion(), "请求错误！参数[version]不能为空");
        IamMenu oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        IamMenu.copyIfNotNull(entity, oldEntity);
        updateByIdSelective(oldEntity);
        return oldEntity;
    }

    public IamMenu save(IamMenu entity) {
        return entity.getId() == null ? create(entity) : update(entity);
    }

    private void duplicateCheck(IamMenu entity) {
        // 唯一条件为空，直接通过
        if (true) {
            return;
        }
        
        // 唯一条件不为空，请设置唯一条件
        IamMenu param = new IamMenu();
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

