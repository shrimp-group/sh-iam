package com.wkclz.iam.admin.service;

import com.wkclz.core.base.PageData;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.mapper.IamApiFieldMapper;
import com.wkclz.iam.admin.mapper.IamApiMapper;
import com.wkclz.iam.admin.mapper.IamMenuFieldMapper;
import com.wkclz.iam.common.dto.IamApiFieldDto;
import com.wkclz.iam.common.entity.IamApi;
import com.wkclz.iam.common.entity.IamApiField;
import com.wkclz.mybatis.helper.PageQuery;
import com.wkclz.mybatis.service.BaseService;
import com.wkclz.redis.helper.RedisIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author shrimp
 * @table iam_api_field (API字段权限) 单表服务类
 */
@Service
public class IamApiFieldService extends BaseService<IamApiField, IamApiFieldMapper> {

    private static final Logger log = LoggerFactory.getLogger(IamApiFieldService.class);

    @Autowired
    private IamApiMapper iamApiMapper;
    @Autowired
    private RedisIdGenerator redisIdGenerator;
    @Autowired
    private IamMenuFieldMapper iamMenuFieldMapper;

    /**
     * 分页查询API字段权限列表
     */
    public PageData<IamApiFieldDto> getApiFieldPage(IamApiFieldDto entity) {
        return PageQuery.page(entity, mapper::getApiFieldList);
    }

    /**
     * 按 API 编码查询字段权限列表
     */
    public List<IamApiField> listByApi(String apiCode) {
        log.info("按API查询字段权限列表, apiCode={}", apiCode);
        IamApiField param = new IamApiField();
        param.setApiCode(apiCode);
        return selectByEntity(param);
    }

    /**
     * 创建API字段权限
     */
    @Transactional(rollbackFor = Exception.class)
    public IamApiField create(IamApiField entity) {
        log.info("创建API字段权限, appCode={}, apiCode={}, fieldName={}", entity.getAppCode(), entity.getApiCode(), entity.getFieldName());

        // apiCode 存在性校验
        IamApi apiParam = new IamApi();
        apiParam.setApiCode(entity.getApiCode());
        IamApi api = iamApiMapper.selectOneByEntity(apiParam);
        if (api == null) {
            throw ValidationException.of("API编码不存在");
        }

        // 唯一性校验: appCode + fieldCode（创建时 fieldCode 由系统生成，此处校验 appCode + fieldName 组合避免重复定义）
        duplicateCheck(entity);

        // action=MASK 时 maskRule 必填校验
        if ("MASK".equals(entity.getAction()) && StringUtils.isBlank(entity.getMaskRule())) {
            throw ValidationException.of("动作类型为脱敏时，脱敏规则不能为空");
        }

        // 生成 fieldCode
        entity.setFieldCode(redisIdGenerator.generateIdWithPrefix("field_"));
        mapper.insert(entity);
        log.info("API字段权限创建成功, fieldCode={}", entity.getFieldCode());
        return entity;
    }

    /**
     * 更新API字段权限
     */
    @Transactional(rollbackFor = Exception.class)
    public IamApiField update(IamApiField entity) {
        log.info("更新API字段权限, id={}, fieldCode={}", entity.getId(), entity.getFieldCode());

        IamApiField oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }

        // 唯一性校验
        duplicateCheck(entity);

        // action=MASK 时 maskRule 必填校验
        if ("MASK".equals(entity.getAction() != null ? entity.getAction() : oldEntity.getAction())) {
            String maskRule = entity.getMaskRule() != null ? entity.getMaskRule() : oldEntity.getMaskRule();
            if (StringUtils.isBlank(maskRule)) {
                throw ValidationException.of("动作类型为脱敏时，脱敏规则不能为空");
            }
        }

        IamApiField.copyIfNotNull(entity, oldEntity);
        updateByIdSelective(oldEntity);
        log.info("API字段权限更新成功, id={}", entity.getId());
        return oldEntity;
    }

    /**
     * 删除API字段权限（同时清理 iam_menu_field 关联记录）
     */
    @Transactional(rollbackFor = Exception.class)
    public IamApiField remove(IamApiField entity) {
        IamApiField oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }

        log.info("删除API字段权限, id={}, fieldCode={}", entity.getId(), oldEntity.getFieldCode());

        // 清理 iam_menu_field 关联记录
        iamMenuFieldMapper.deleteByFieldCode(oldEntity.getFieldCode());
        log.info("已清理菜单字段关联记录, fieldCode={}", oldEntity.getFieldCode());

        deleteById(oldEntity);
        log.info("API字段权限删除成功, fieldCode={}", oldEntity.getFieldCode());
        return oldEntity;
    }

    /**
     * 唯一性校验: appCode + fieldCode
     */
    private void duplicateCheck(IamApiField entity) {
        if (StringUtils.isBlank(entity.getAppCode()) || StringUtils.isBlank(entity.getFieldCode())) {
            return;
        }

        IamApiField param = new IamApiField();
        param.setAppCode(entity.getAppCode());
        param.setFieldCode(entity.getFieldCode());
        param = selectOneByEntity(param);
        if (param == null) {
            return;
        }
        if (param.getId().equals(entity.getId())) {
            return;
        }
        throw UserException.of(ResultCode.RECORD_DUPLICATE);
    }

}
