package com.wkclz.iam.admin.service;

import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.mapper.IamMenuApiMapper;
import com.wkclz.iam.admin.bean.resp.ApiBoundResp;
import com.wkclz.iam.common.entity.IamMenuApi;
import com.wkclz.mybatis.service.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_menu_api (菜单 接口) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_menu_api 的逻辑. 其他逻辑放 custom 中
 */

@Service
public class IamMenuApiService extends BaseService<IamMenuApi, IamMenuApiMapper> {

    private static final Logger log = LoggerFactory.getLogger(IamMenuApiService.class);


    public List<IamMenuApi> getMenuList(String menuCode) {
        IamMenuApi param = new IamMenuApi();
        param.setMenuCode(menuCode);
        return selectByEntity(param);
    }

    /**
     * 查询菜单已绑定的 API 详情列表
     *
     * @param menuCode 菜单编码
     * @return API 绑定信息列表
     */
    public List<ApiBoundResp> getBoundApis(String menuCode) {
        log.info("查询菜单已绑定API列表, menuCode={}", menuCode);
        return mapper.getBoundApis(menuCode);
    }

    public IamMenuApi create(IamMenuApi entity) {
        duplicateCheck(entity);
        mapper.insert(entity);
        log.info("菜单-API绑定成功, menuCode={}, apiCode={}", entity.getMenuCode(), entity.getApiCode());
        return entity;
    }

    public IamMenuApi remove(IamMenuApi entity) {
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        IamMenuApi oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        deleteById(oldEntity);
        log.info("菜单-API解绑成功, menuCode={}, apiCode={}", oldEntity.getMenuCode(), oldEntity.getApiCode());
        return oldEntity;
    }

    private void duplicateCheck(IamMenuApi entity) {
        // 唯一条件为空，直接通过

        // 唯一条件不为空，请设置唯一条件
        IamMenuApi param = new IamMenuApi();
        param.setAppCode(entity.getAppCode());
        param.setMenuCode(entity.getMenuCode());
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

