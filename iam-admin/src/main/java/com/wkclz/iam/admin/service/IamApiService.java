package com.wkclz.iam.admin.service;

import com.alibaba.fastjson2.JSON;
import com.wkclz.core.base.PageData;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.bean.resp.*;
import com.wkclz.iam.admin.helper.EntityFieldAnalyzer;
import com.wkclz.iam.admin.mapper.IamApiMapper;
import com.wkclz.iam.admin.mapper.IamMenuApiMapper;
import com.wkclz.iam.common.dto.IamApiDto;
import com.wkclz.iam.common.entity.IamApi;
import com.wkclz.mybatis.helper.PageQuery;
import com.wkclz.mybatis.service.BaseService;
import com.wkclz.redis.helper.RedisIdGenerator;
import com.wkclz.tool.utils.ClassTypeHelper;
import com.wkclz.web.bean.RestField;
import com.wkclz.web.bean.RestInfo;
import com.wkclz.web.bean.RestParam;
import com.wkclz.web.helper.RestHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
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
    @Autowired
    private EntityFieldAnalyzer entityFieldAnalyzer;

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


    /**
     * 获取接口文档信息
     * 通过 RestHelper 匹配接口，提取请求参数和返回参数
     */
    public ApiDocResp getApiDoc(String method, String uri) {
        log.info("获取接口文档信息, method: {}, uri: {}", method, uri);
        ApiDocResp docResp = new ApiDocResp();
        docResp.setMethod(method);
        docResp.setUri(uri);

        // 通过 RestHelper 获取所有接口映射
        List<RestInfo> mappings = RestHelper.getMapping();
        RestInfo matched = null;
        for (RestInfo info : mappings) {
            if (method.equalsIgnoreCase(info.getMethod()) && uri.equals(info.getUri())) {
                matched = info;
                break;
            }
        }

        if (matched == null) {
            log.warn("未匹配到接口, method: {}, uri: {}", method, uri);
            return docResp;
        }

        docResp.setName(matched.getName());
        docResp.setDescription(matched.getDesc());
        docResp.setTag(matched.getTag());
        docResp.setOperationSummary(matched.getOperationSummary());
        docResp.setOperationDescription(matched.getOperationDescription());
        docResp.setDeprecated(matched.getDeprecated());
        docResp.setConsumes(matched.getConsumes());
        docResp.setProduces(matched.getProduces());

        // 处理请求参数
        if (matched.getParameters() != null) {
            List<ApiDocParamResp> paramResps = new ArrayList<>();
            for (RestParam param : matched.getParameters()) {
                ApiDocParamResp paramResp = new ApiDocParamResp();
                paramResp.setName(param.getName());
                paramResp.setType(param.getType());
                paramResp.setAnnotationType(param.getAnnotationType());
                paramResp.setRequired(param.getRequired());
                paramResp.setDefaultValue(param.getDefaultValue());
                paramResp.setDescription(param.getDescription());
                paramResp.setExample(param.getExample());
                paramResp.setRequiredMode(param.getRequiredMode());

                // 对复杂类型参数，使用 RestHelper 扫描的字段结构
                if (param.getFields() != null && !param.getFields().isEmpty()) {
                    paramResp.setFields(convertRestFields(param.getFields(), param.getName()));
                }
                paramResps.add(paramResp);
            }
            docResp.setRequestParams(paramResps);
        }

        // 处理返回参数 - 优先使用 EntityFieldAnalyzer 构建树形结构（保留 R/PageData 包装层）
        if (matched.getReturnGenericInfo() != null) {
            try {
                List<EntityFieldNode> returnFieldNodes = entityFieldAnalyzer.buildReturnFieldTree(matched.getReturnGenericInfo());
                docResp.setReturnSchemaFields(convertEntityFieldNodes(returnFieldNodes, null));
            } catch (Exception e) {
                log.warn("解析返回参数字段树失败, error: {}", e.getMessage());
            }
        }

        // 用 returnSchema 丰富字段详情（description/example/required）
        if (matched.getReturnSchema() != null && docResp.getReturnSchemaFields() != null) {
            try {
                List<RestField> schemaFields = JSON.parseArray(matched.getReturnSchema(), RestField.class);
                enrichFieldsFromSchema(docResp.getReturnSchemaFields(), schemaFields);
            } catch (Exception e) {
                log.warn("丰富返回参数字段详情失败, error: {}", e.getMessage());
            }
        }

        // 兼容回退：保留 returnFields（EntityFieldNode 原始结构）
        if (matched.getReturnGenericInfo() != null) {
            try {
                List<EntityFieldNode> returnFields = entityFieldAnalyzer.buildReturnFieldTree(matched.getReturnGenericInfo());
                docResp.setReturnFields(returnFields);
            } catch (Exception e) {
                log.warn("解析返回参数字段树失败, error: {}", e.getMessage());
            }
        }

        return docResp;
    }

    /**
     * 将 RestField 列表递归转换为 ApiDocFieldResp 列表
     *
     * @param restFields RestField 列表
     * @param parentKey  父级 key，用于生成唯一路径标识
     */
    private List<ApiDocFieldResp> convertRestFields(List<RestField> restFields, String parentKey) {
        if (restFields == null || restFields.isEmpty()) {
            return null;
        }
        List<ApiDocFieldResp> result = new ArrayList<>();
        for (RestField field : restFields) {
            ApiDocFieldResp resp = new ApiDocFieldResp();

            // 生成唯一 key
            String fieldKey;
            if (parentKey == null || parentKey.isEmpty()) {
                fieldKey = field.getName();
            } else {
                fieldKey = parentKey + "." + field.getName();
            }
            resp.setKey(fieldKey);

            resp.setName(field.getName());
            resp.setType(field.getType());
            resp.setSimpleType(field.getSimpleType());
            resp.setDescription(field.getDescription());
            resp.setExample(field.getExample());
            resp.setRequired(field.getRequired());
            resp.setGenericTypes(field.getGenericTypes());
            resp.setSelfReferencing(field.getSelfReferencing());
            if (field.getFields() != null && !field.getFields().isEmpty()) {
                // 对于自引用类型，使用 null 作为 childParentKey 避免无限递归
                String childParentKey = Boolean.TRUE.equals(field.getSelfReferencing()) ? null : fieldKey;
                resp.setFields(convertRestFields(field.getFields(), childParentKey));
            }
            result.add(resp);
        }
        return result;
    }

    /**
     * 将 EntityFieldNode 列表递归转换为 ApiDocFieldResp 列表
     * 保留 R/PageData 包装层的树形结构
     *
     * @param nodes     EntityFieldNode 列表
     * @param parentKey 父级 key，用于生成唯一路径标识
     */
    private List<ApiDocFieldResp> convertEntityFieldNodes(List<EntityFieldNode> nodes, String parentKey) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        List<ApiDocFieldResp> result = new ArrayList<>();
        for (EntityFieldNode node : nodes) {
            ApiDocFieldResp resp = new ApiDocFieldResp();

            // 生成唯一 key（使用 jsonPath 如果可用，否则基于路径）
            String fieldKey = node.getJsonPath() != null ? node.getJsonPath() :
                (parentKey == null || parentKey.isEmpty() ? node.getFieldName() : parentKey + "." + node.getFieldName());
            resp.setKey(fieldKey);

            resp.setName(node.getFieldName());
            resp.setDescription(node.getFieldDesc());
            resp.setType(node.getFieldType());
            resp.setSimpleType(ClassTypeHelper.isSimpleType(node.getFieldType()));

            // 递归转换子字段
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                resp.setFields(convertEntityFieldNodes(node.getChildren(), fieldKey));
            }

            result.add(resp);
        }
        return result;
    }

    /**
     * 用 returnSchema 中的信息丰富 ApiDocFieldResp 字段详情
     * 根据 name 匹配，补充 description/example/required
     */
    private void enrichFieldsFromSchema(List<ApiDocFieldResp> fields, List<RestField> schemaFields) {
        if (fields == null || schemaFields == null) {
            return;
        }

        // Build a map of schema fields by name for quick lookup
        Map<String, RestField> schemaMap = new HashMap<>();
        for (RestField sf : schemaFields) {
            schemaMap.put(sf.getName(), sf);
        }

        for (ApiDocFieldResp field : fields) {
            RestField schemaField = schemaMap.get(field.getName());
            if (schemaField != null) {
                // Enrich with schema info if current field is missing it
                if (field.getDescription() == null && schemaField.getDescription() != null) {
                    field.setDescription(schemaField.getDescription());
                }
                if (field.getExample() == null && schemaField.getExample() != null) {
                    field.setExample(schemaField.getExample());
                }
                if (field.getRequired() == null && schemaField.getRequired() != null) {
                    field.setRequired(schemaField.getRequired());
                }
            }
            // Recurse into children
            if (field.getFields() != null && !field.getFields().isEmpty()) {
                enrichFieldsFromSchema(field.getFields(), schemaFields);
            }
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

