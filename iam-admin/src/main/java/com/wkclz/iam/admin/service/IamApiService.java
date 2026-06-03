package com.wkclz.iam.admin.service;

import com.wkclz.core.base.PageData;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.mapper.IamApiMapper;
import com.wkclz.iam.admin.mapper.IamMenuApiMapper;
import com.wkclz.iam.admin.bean.resp.ApiDetailResp;
import com.wkclz.iam.common.dto.IamApiDto;
import com.wkclz.iam.common.entity.IamApi;
import com.wkclz.mybatis.helper.PageQuery;
import com.wkclz.mybatis.service.BaseService;
import com.wkclz.redis.helper.RedisIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_api (路由映射) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_api 的逻辑. 其他逻辑放 custom 中
 */
 
@Service
public class IamApiService extends BaseService<IamApi, IamApiMapper> {

    private static final Logger log = LoggerFactory.getLogger(IamApiService.class);

    @Autowired
    private RedisIdGenerator redisIdGenerator;
    @Autowired
    private IamMenuApiMapper iamMenuApiMapper;

    public PageData<IamApiDto> getApiPage(IamApiDto entity) {
        return PageQuery.page(entity, mapper::getApiList);
    }

    public List<IamApi> getApiOptions(IamApi entity) {
        return mapper.getApiOptions(entity);
    }

    public List<IamApi> getApis4Copy(IamApi entity) {
        return mapper.getApis4Copy(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public Integer apiPaste(List<IamApi> entitys) {
        if (entitys == null || entitys.isEmpty()) {
            return 0;
        }
        
        // 收集所有apiCode
        List<String> apiCodes = entitys.stream()
            .map(IamApi::getApiCode)
            .toList();

        List<IamApi> existingApis = mapper.getApis4Paste(apiCodes);

        // 新增的
        List<String> existingApiCodes = existingApis.stream().map(IamApi::getApiCode).toList();
        List<IamApi> inserts = entitys.stream().filter(t -> !existingApiCodes.contains(t.getApiCode())).toList();

        // 修改的
        List<IamApi> updates = new ArrayList<>();
        Map<String, IamApi> existingApiMap = existingApis.stream().collect(Collectors.toMap(IamApi::getApiCode, t -> t));
        for (IamApi entity : entitys) {
            IamApi existingApi = existingApiMap.get(entity.getApiCode());
            if (needUpdate(entity, existingApi)) {
                updates.add(existingApi);
            }
        }
        if (!CollectionUtils.isEmpty(inserts)) {
            mapper.insertBatch(inserts);
        }

        if (!CollectionUtils.isEmpty(updates)) {
            for (IamApi update : updates) {
                mapper.updateById(update);
            }
        }
        return inserts.size() + updates.size();
    }

    private List<IamApiDto> getAllApis() {
        // 查询所有接口，一次性批量查询
        IamApiDto param = new IamApiDto();
        return mapper.getApiList(param);
    }
    
    private boolean needUpdate(IamApi newEntity, IamApi oldEntity) {
        boolean chageFlag = false;
        // 对比字段，只有在字段有变化时才需要修改
        if (!StringUtils.equals(newEntity.getModule(), oldEntity.getModule())) {
            oldEntity.setModule(newEntity.getModule());
            chageFlag = true;
        }
        if (!StringUtils.equals(newEntity.getAppCode(), oldEntity.getAppCode())) {
            oldEntity.setAppCode(newEntity.getAppCode());
            chageFlag = true;
        }
        if (!StringUtils.equals(newEntity.getApiMethod(), oldEntity.getApiMethod())) {
            oldEntity.setApiMethod(newEntity.getApiMethod());
            chageFlag = true;
        }
        if (!StringUtils.equals(newEntity.getApiUri(), oldEntity.getApiUri())) {
            oldEntity.setApiUri(newEntity.getApiUri());
            chageFlag = true;
        }
        if (!StringUtils.equals(newEntity.getApiName(), oldEntity.getApiName())) {
            oldEntity.setApiName(newEntity.getApiName());
            chageFlag = true;
        }
        if (!Objects.equals(newEntity.getWriteFlag(), oldEntity.getWriteFlag())) {
            oldEntity.setWriteFlag(newEntity.getWriteFlag());
            chageFlag = true;
        }
        if (!StringUtils.equals(newEntity.getRemark(), oldEntity.getRemark())) {
            oldEntity.setRemark(newEntity.getRemark());
            chageFlag = true;
        }
        return chageFlag;
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

    /**
     * 查询 API 详情，包含已绑定的菜单全路径
     *
     * @param id API 主键ID
     * @return API 详情响应
     */
    public ApiDetailResp getApiDetail(Long id) {
        log.info("查询API详情, id={}", id);
        IamApi api = selectById(id);
        if (api == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }

        ApiDetailResp resp = new ApiDetailResp();
        BeanUtils.copyProperties(api, resp);

        // 使用 RECURSIVE CTE 一次性查询所有已绑定菜单的全路径，避免 N+1 查询
        List<String> boundMenuPaths = iamMenuApiMapper.getBoundMenuPathsByApiCode(api.getApiCode());
        resp.setBoundMenuPaths(boundMenuPaths);
        log.info("API详情查询完成, apiCode={}, 绑定菜单数={}", api.getApiCode(), boundMenuPaths.size());
        return resp;
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

