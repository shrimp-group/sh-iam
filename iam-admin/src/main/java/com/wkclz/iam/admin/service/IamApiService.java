package com.wkclz.iam.admin.service;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.wkclz.core.base.PageData;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.bean.req.ApiBatchCreateReq;
import com.wkclz.iam.admin.bean.req.ApiExportReq;
import com.wkclz.iam.admin.bean.req.ApiImportReq;
import com.wkclz.iam.admin.bean.req.ApiScanReq;
import com.wkclz.iam.admin.bean.resp.*;
import com.wkclz.iam.admin.helper.EntityFieldAnalyzer;
import com.wkclz.iam.admin.mapper.IamApiFieldMapper;
import com.wkclz.iam.admin.mapper.IamApiMapper;
import com.wkclz.iam.admin.mapper.IamMenuApiMapper;
import com.wkclz.iam.session.bean.ApiRequestControl;
import com.wkclz.iam.common.dto.IamApiDto;
import com.wkclz.iam.common.entity.IamApi;
import com.wkclz.iam.session.event.PermissionChangeEvent;
import com.wkclz.iam.common.entity.IamApiField;
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
import org.springframework.context.ApplicationEventPublisher;
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

    /**
     * 导入批量分块大小，避免单 SQL 过大
     */
    private static final int IMPORT_BATCH_SIZE = 200;

    /**
     * 字段权限合法动作类型
     */
    private static final Set<String> FIELD_ACTIONS = Set.of("HIDDEN", "MASK", "READ_ONLY");

    @Autowired
    private RedisIdGenerator redisIdGenerator;
    @Autowired
    private IamMenuApiMapper iamMenuApiMapper;
    @Autowired
    private IamApiFieldMapper iamApiFieldMapper;
    @Autowired
    private EntityFieldAnalyzer entityFieldAnalyzer;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

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
        // 按粘贴涉及的应用发布权限变更
        entitys.stream().map(IamApi::getAppCode)
            .filter(code -> code != null && !code.isEmpty()).distinct()
            .forEach(this::publishAppChanged);
        return inserts.size() + updates.size();
    }

    /**
     * 发布应用静态资源变更事件（appCode 为空跳过）
     */
    private void publishAppChanged(String appCode) {
        if (appCode == null || appCode.isEmpty()) {
            log.warn("appCode 为空，跳过权限变更发布");
            return;
        }
        eventPublisher.publishEvent(PermissionChangeEvent.appChanged(appCode));
        log.info("已发布权限变更事件: type=APP_STATIC, key={}", appCode);
    }

    private List<IamApiDto> getAllApis() {
        // 查询所有接口，一次性批量查询
        IamApiDto param = new IamApiDto();
        return mapper.getApiList(param);
    }

    /**
     * 导出 API 及其字段权限：ids 有值按主键批量导出（选中项），否则按筛选条件全量导出（与页面查询同语义，无分页）
     *
     * @param req 导出请求
     * @return 导出数据（业务字段 + requestControl + fields）
     */
    public List<ApiExportResp> getApis4Export(ApiExportReq req) {
        log.info("导出API, ids={}, appCode={}", req.getIds(), req.getAppCode());

        // 主数据一次批量查询：选中项按主键，全量按筛选条件
        List<? extends IamApi> apis;
        if (!CollectionUtils.isEmpty(req.getIds())) {
            apis = mapper.selectByIds(req.getIds());
        } else {
            IamApiDto param = new IamApiDto();
            param.setModule(req.getModule());
            param.setAppCode(req.getAppCode());
            param.setApiCode(req.getApiCode());
            param.setApiMethod(req.getApiMethod());
            param.setApiUri(req.getApiUri());
            param.setApiName(req.getApiName());
            param.setWriteFlag(req.getWriteFlag());
            param.setMenuBindStatus(req.getMenuBindStatus());
            apis = mapper.getApiList(param);
        }
        if (apis.isEmpty()) {
            return Collections.emptyList();
        }

        // 字段权限一次 IN 批量查询，内存按 apiCode 分组
        List<String> apiCodes = apis.stream().map(IamApi::getApiCode).toList();
        Map<String, List<IamApiField>> fieldMap = iamApiFieldMapper.getFieldsByApiCodes(apiCodes).stream().collect(Collectors.groupingBy(IamApiField::getApiCode));

        List<ApiExportResp> respList = new ArrayList<>(apis.size());
        for (IamApi api : apis) {
            ApiExportResp resp = buildExportResp(api);
            resp.setFields(buildExportFields(fieldMap.get(api.getApiCode())));
            respList.add(resp);
        }
        log.info("导出API完成, api数={}, 字段权限数={}", respList.size(), fieldMap.values().stream().mapToInt(List::size).sum());
        return respList;
    }

    /**
     * 导入 API 及其字段权限：按 apiCode 判断存在性，存在更新、不存在新增，全程无删除
     *
     * @param req 导入请求
     * @return 新增/更新条数明细
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiImportResp apiImport(ApiImportReq req) {
        log.info("导入API, 条数={}", req.getApis().size());

        // 内存校验全部通过后才落库
        validateImport(req.getApis());

        // 一次 IN 批量查询已存在数据，内存划分新增/更新
        List<String> apiCodes = req.getApis().stream().map(ApiImportReq.ApiImportItemReq::getApiCode).toList();
        Map<String, IamApi> existingMap = mapper.getApis4Paste(apiCodes).stream().collect(Collectors.toMap(IamApi::getApiCode, t -> t, (a, b) -> a));

        List<IamApi> inserts = new ArrayList<>();
        List<IamApi> updates = new ArrayList<>();
        for (ApiImportReq.ApiImportItemReq item : req.getApis()) {
            IamApi existing = existingMap.get(item.getApiCode());
            IamApi entity = buildImportEntity(item);
            if (existing == null) {
                inserts.add(entity);
            } else {
                entity.setId(existing.getId());
                updates.add(entity);
            }
        }

        // 字段权限：按 (apiCode + fieldCode) 匹配，存在更新、不存在以导入的 fieldCode 新增，全程无删除
        List<IamApiField> fieldInserts = new ArrayList<>();
        List<IamApiField> fieldUpdates = new ArrayList<>();
        List<IamApiField> importFields = req.getApis().stream()
            .filter(item -> !CollectionUtils.isEmpty(item.getFields()))
            .flatMap(item -> item.getFields().stream().map(f -> buildImportField(f, item)))
            .toList();
        if (!importFields.isEmpty()) {
            List<String> fieldApiCodes = importFields.stream().map(IamApiField::getApiCode).distinct().toList();
            Map<String, IamApiField> existingFieldMap = iamApiFieldMapper.getFieldsByApiCodes(fieldApiCodes).stream()
                .collect(Collectors.toMap(f -> f.getApiCode() + "|" + f.getFieldCode(), f -> f, (a, b) -> a));
            for (IamApiField field : importFields) {
                IamApiField existing = existingFieldMap.get(field.getApiCode() + "|" + field.getFieldCode());
                if (existing == null) {
                    fieldInserts.add(field);
                } else {
                    field.setId(existing.getId());
                    fieldUpdates.add(field);
                }
            }
        }

        // 分块批量落库，避免单 SQL 过大
        for (List<IamApi> chunk : Lists.partition(inserts, IMPORT_BATCH_SIZE)) {
            mapper.insertBatch(chunk);
        }
        for (List<IamApi> chunk : Lists.partition(updates, IMPORT_BATCH_SIZE)) {
            mapper.updateBatch4Import(chunk);
        }
        for (List<IamApiField> chunk : Lists.partition(fieldInserts, IMPORT_BATCH_SIZE)) {
            iamApiFieldMapper.insertBatch(chunk);
        }
        for (List<IamApiField> chunk : Lists.partition(fieldUpdates, IMPORT_BATCH_SIZE)) {
            iamApiFieldMapper.updateBatch4Import(chunk);
        }

        ApiImportResp resp = new ApiImportResp();
        resp.setInsertApiCount((long) inserts.size());
        resp.setUpdateApiCount((long) updates.size());
        resp.setInsertFieldCount((long) fieldInserts.size());
        resp.setUpdateFieldCount((long) fieldUpdates.size());
        log.info("导入API完成, 新增API={}, 更新API={}, 新增字段={}, 更新字段={}", resp.getInsertApiCount(), resp.getUpdateApiCount(), resp.getInsertFieldCount(), resp.getUpdateFieldCount());
        // 按导入涉及的应用发布权限变更
        req.getApis().stream().map(ApiImportReq.ApiImportItemReq::getAppCode)
            .filter(code -> code != null && !code.isEmpty()).distinct()
            .forEach(this::publishAppChanged);
        return resp;
    }

    /**
     * 导入数据校验：逐条定位缺失项，任一不合法则整体不通过
     */
    private void validateImport(List<ApiImportReq.ApiImportItemReq> items) {
        List<String> errors = new ArrayList<>();
        Set<String> apiCodeSet = new HashSet<>();
        Set<String> fieldKeySet = new HashSet<>();
        // method+uri 全局唯一校验（导入项间 + 与库中冲突）
        duplicateCheck4Import(items, errors);
        for (int i = 0; i < items.size(); i++) {
            ApiImportReq.ApiImportItemReq item = items.get(i);
            String tag = "第" + (i + 1) + "条";
            if (StringUtils.isBlank(item.getAppCode())) {
                errors.add(tag + "：缺少应用编码");
            }
            if (StringUtils.isBlank(item.getApiCode())) {
                errors.add(tag + "：缺少接口编码");
            }
            if (StringUtils.isBlank(item.getApiMethod())) {
                errors.add(tag + "：缺少请求方法");
            }
            if (StringUtils.isBlank(item.getApiUri())) {
                errors.add(tag + "：缺少请求URI");
            }
            if (StringUtils.isNotBlank(item.getApiCode()) && !apiCodeSet.add(item.getApiCode())) {
                errors.add(tag + "：接口编码 " + item.getApiCode() + " 重复出现");
            }
            if (CollectionUtils.isEmpty(item.getFields())) {
                continue;
            }
            for (int j = 0; j < item.getFields().size(); j++) {
                ApiImportReq.ApiImportFieldReq field = item.getFields().get(j);
                String fieldTag = tag + "字段权限第" + (j + 1) + "项";
                if (StringUtils.isBlank(field.getFieldCode())) {
                    errors.add(fieldTag + "：缺少字段编码");
                }
                if (StringUtils.isBlank(field.getFieldName())) {
                    errors.add(fieldTag + "：缺少字段名称");
                }
                if (StringUtils.isBlank(field.getJsonPath())) {
                    errors.add(fieldTag + "：缺少JSON路径");
                }
                if (StringUtils.isBlank(field.getAction())) {
                    errors.add(fieldTag + "：缺少权限动作");
                } else if (!FIELD_ACTIONS.contains(field.getAction())) {
                    errors.add(fieldTag + "：权限动作 " + field.getAction() + " 不合法，应为 HIDDEN/MASK/READ_ONLY");
                }
                if ("MASK".equals(field.getAction()) && StringUtils.isBlank(field.getMaskRule())) {
                    errors.add(fieldTag + "：动作类型为脱敏时，脱敏规则不能为空");
                }
                if (StringUtils.isNotBlank(item.getApiCode()) && StringUtils.isNotBlank(field.getFieldCode())
                    && !fieldKeySet.add(item.getApiCode() + "|" + field.getFieldCode())) {
                    errors.add(fieldTag + "：字段编码 " + field.getFieldCode() + " 重复出现");
                }
            }
        }
        if (!errors.isEmpty()) {
            log.warn("导入API校验失败, 错误数={}", errors.size());
            throw ValidationException.of(String.join("；", errors));
        }
    }

    private ApiExportResp buildExportResp(IamApi api) {
        ApiExportResp resp = new ApiExportResp();
        resp.setModule(api.getModule());
        resp.setAppCode(api.getAppCode());
        resp.setApiCode(api.getApiCode());
        resp.setApiMethod(api.getApiMethod());
        resp.setApiUri(api.getApiUri());
        resp.setApiName(api.getApiName());
        resp.setWriteFlag(api.getWriteFlag());
        resp.setRequestControl(ApiRequestControl.parse(api.getRequestControl()));
        resp.setRemark(api.getRemark());
        resp.setSort(api.getSort());
        return resp;
    }

    private List<ApiExportResp.ApiFieldExportResp> buildExportFields(List<IamApiField> fields) {
        if (CollectionUtils.isEmpty(fields)) {
            return Collections.emptyList();
        }
        List<ApiExportResp.ApiFieldExportResp> respList = new ArrayList<>(fields.size());
        for (IamApiField field : fields) {
            ApiExportResp.ApiFieldExportResp resp = new ApiExportResp.ApiFieldExportResp();
            resp.setFieldCode(field.getFieldCode());
            resp.setFieldName(field.getFieldName());
            resp.setJsonPath(field.getJsonPath());
            resp.setAction(field.getAction());
            resp.setMaskRule(field.getMaskRule());
            resp.setDescription(field.getDescription());
            resp.setSort(field.getSort());
            respList.add(resp);
        }
        return respList;
    }

    private IamApi buildImportEntity(ApiImportReq.ApiImportItemReq item) {
        IamApi entity = new IamApi();
        entity.setModule(item.getModule());
        entity.setAppCode(item.getAppCode());
        entity.setApiCode(item.getApiCode());
        entity.setApiMethod(item.getApiMethod());
        entity.setApiUri(item.getApiUri());
        entity.setApiName(item.getApiName());
        entity.setWriteFlag(item.getWriteFlag());
        entity.setRequestControl(item.getRequestControl() == null ? null : JSON.toJSONString(item.getRequestControl()));
        entity.setRemark(item.getRemark());
        entity.setSort(item.getSort());
        return entity;
    }

    /**
     * 字段权限导入项转实体，appCode/apiCode 继承所属 API
     */
    private IamApiField buildImportField(ApiImportReq.ApiImportFieldReq fieldReq, ApiImportReq.ApiImportItemReq item) {
        IamApiField entity = new IamApiField();
        entity.setAppCode(item.getAppCode());
        entity.setApiCode(item.getApiCode());
        entity.setFieldCode(fieldReq.getFieldCode());
        entity.setFieldName(fieldReq.getFieldName());
        entity.setJsonPath(fieldReq.getJsonPath());
        entity.setAction(fieldReq.getAction());
        entity.setMaskRule(fieldReq.getMaskRule());
        entity.setDescription(fieldReq.getDescription());
        entity.setSort(fieldReq.getSort());
        return entity;
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
        // requestControl 为 JSON 字符串，按规范化后对比，忽略字段顺序等差异
        if (!StringUtils.equals(normalizeJson(newEntity.getRequestControl()), normalizeJson(oldEntity.getRequestControl()))) {
            oldEntity.setRequestControl(newEntity.getRequestControl());
            chageFlag = true;
        }
        if (!StringUtils.equals(newEntity.getRemark(), oldEntity.getRemark())) {
            oldEntity.setRemark(newEntity.getRemark());
            chageFlag = true;
        }
        return chageFlag;
    }

    /**
     * JSON 字符串规范化，用于忽略字段顺序等差异后对比；空白串返回空串，解析失败返回原串
     *
     * @param json JSON 字符串
     * @return 规范化后的字符串
     */
    private String normalizeJson(String json) {
        if (StringUtils.isBlank(json)) {
            return "";
        }
        try {
            return JSON.toJSONString(JSON.parse(json));
        } catch (Exception e) {
            return json;
        }
    }


    @Transactional(rollbackFor = Exception.class)
    public IamApi create(IamApi entity) {
        log.info("创建API, entity={}", entity);
        if (StringUtils.isBlank(entity.getApiCode())) {
            entity.setApiCode(redisIdGenerator.generateBase32IdWithPrefix("api_"));
        }
        duplicateCheck(entity);
        mapper.insert(entity);
        publishAppChanged(entity.getAppCode());
        return entity;
    }

    @Transactional(rollbackFor = Exception.class)
    public IamApi update(IamApi entity) {
        log.info("更新API, entity={}", entity);
        duplicateCheck(entity);
        IamApi oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        // 编码不可维护
        entity.setApiCode(null);
        IamApi.copyIfNotNull(entity, oldEntity);
        updateByIdSelective(oldEntity);
        publishAppChanged(oldEntity.getAppCode());
        return oldEntity;
    }

    @Transactional(rollbackFor = Exception.class)
    public IamApi save(IamApi entity) {
        return entity.getId() == null ? create(entity) : update(entity);
    }

    @Transactional(rollbackFor = Exception.class)
    public IamApi remove(IamApi entity) {
        IamApi oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        deleteById(oldEntity);
        publishAppChanged(oldEntity.getAppCode());
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

        // requestControl 为 JSON 字符串，解析为对象返回；空白或解析失败返回 null
        resp.setRequestControl(ApiRequestControl.parse(api.getRequestControl()));

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
     * 扫描运行时代码中的 REST 接口，按条件过滤后标记是否已在库
     *
     * @param req 扫描过滤条件
     * @return 过滤后的扫描结果列表
     */
    public List<ApiScanResp> scanApis(ApiScanReq req) {
        log.info("扫描API列表, 过滤条件={}", req);
        List<RestInfo> mappings = RestHelper.getMapping();

        // 第一步：按扫描结果自身字段过滤（appCode/module/apiMethod/apiUri/apiName），减少后续查库范围
        List<RestInfo> filtered = new ArrayList<>(mappings.size());
        for (RestInfo info : mappings) {
            if (StringUtils.isNotBlank(req.getAppCode()) && !req.getAppCode().equals(info.getAppCode())) {
                continue;
            }
            if (StringUtils.isNotBlank(req.getModule()) && !StringUtils.contains(info.getModule(), req.getModule())) {
                continue;
            }
            if (StringUtils.isNotBlank(req.getApiMethod()) && !req.getApiMethod().equalsIgnoreCase(info.getMethod())) {
                continue;
            }
            if (StringUtils.isNotBlank(req.getApiUri()) && !StringUtils.contains(info.getUri(), req.getApiUri())) {
                continue;
            }
            if (StringUtils.isNotBlank(req.getApiName()) && !StringUtils.contains(info.getDesc(), req.getApiName())) {
                continue;
            }
            filtered.add(info);
        }

        // 第二步：按需查库——仅按 api_uri IN 查询过滤后扫描结果对应的记录，避免全量加载
        Set<String> uris = new HashSet<>();
        for (RestInfo info : filtered) {
            uris.add(info.getUri());
        }
        Map<String, Long> existingMap = new HashMap<>();
        if (!uris.isEmpty()) {
            // SQL 仅按 api_uri IN 匹配（可走索引），返回结果中用 method+uri 精确比对
            List<IamApi> iamApis = mapper.listByUris(new ArrayList<>(uris));
            for (IamApi api : iamApis) {
                String key = api.getApiMethod().toUpperCase() + "|" + api.getApiUri();
                existingMap.put(key, api.getId());
            }
        }

        // 第三步：标记 exists，按入库状态过滤，构建响应
        List<ApiScanResp> respList = new ArrayList<>(filtered.size());
        for (RestInfo info : filtered) {
            String key = info.getMethod().toUpperCase() + "|" + info.getUri();
            Long existingId = existingMap.get(key);
            boolean exists = existingId != null;
            if (req.getExists() != null && req.getExists() != exists) {
                continue;
            }
            ApiScanResp resp = new ApiScanResp();
            resp.setAppCode(info.getAppCode());
            resp.setModule(info.getModule());
            resp.setApiMethod(info.getMethod());
            resp.setApiUri(info.getUri());
            resp.setApiName(info.getDesc());
            resp.setWriteFlag(info.getWriteFlag());
            resp.setExists(exists);
            resp.setExistingId(existingId);
            respList.add(resp);
        }
        long existsCount = respList.stream().filter(ApiScanResp::getExists).count();
        log.info("扫描API完成, 扫描总数={}, 过滤后={}, 已入库={}", mappings.size(), respList.size(), existsCount);
        return respList;
    }

    /**
     * 批量新增API：逐条校验 method+uri 全局唯一性，成功项入库，失败项返回原因
     *
     * @param req 批量新增请求
     * @return 成功/失败明细
     */
    public ApiBatchCreateResp batchCreate(ApiBatchCreateReq req) {
        List<ApiBatchCreateReq.ApiBatchCreateItemReq> items = req.getItems();
        log.info("批量新增API, 条数={}", items.size());

        // 按需查库：仅按 api_uri IN 查询本批 URI 对应的记录，避免全量加载
        Set<String> uris = new HashSet<>();
        for (ApiBatchCreateReq.ApiBatchCreateItemReq item : items) {
            if (StringUtils.isNotBlank(item.getApiUri())) {
                uris.add(item.getApiUri());
            }
        }
        Map<String, String> existingKeyMap = new HashMap<>();
        if (!uris.isEmpty()) {
            // SQL 仅按 api_uri IN 匹配（可走索引），返回结果中用 method+uri 精确比对
            for (IamApi api : mapper.listByUris(new ArrayList<>(uris))) {
                existingKeyMap.put(api.getApiMethod().toUpperCase() + "|" + api.getApiUri(), api.getApiCode());
            }
        }

        List<IamApi> inserts = new ArrayList<>();
        List<ApiBatchCreateResp.FailedItem> failedItems = new ArrayList<>();
        Set<String> batchKeySet = new HashSet<>();

        for (ApiBatchCreateReq.ApiBatchCreateItemReq item : items) {
            if (StringUtils.isBlank(item.getApiName())) {
                failedItems.add(buildFailedItem(item, "接口名称不能为空"));
                continue;
            }
            if (StringUtils.isBlank(item.getApiMethod()) || StringUtils.isBlank(item.getApiUri())) {
                failedItems.add(buildFailedItem(item, "请求方法或URI不能为空"));
                continue;
            }
            String key = item.getApiMethod().toUpperCase() + "|" + item.getApiUri();
            if (existingKeyMap.containsKey(key)) {
                failedItems.add(buildFailedItem(item, "接口已存在"));
                continue;
            }
            if (!batchKeySet.add(key)) {
                failedItems.add(buildFailedItem(item, "同批次内重复"));
                continue;
            }
            IamApi entity = new IamApi();
            entity.setModule(item.getModule());
            entity.setAppCode(item.getAppCode());
            entity.setApiMethod(item.getApiMethod());
            entity.setApiUri(item.getApiUri());
            entity.setApiName(item.getApiName());
            entity.setWriteFlag(item.getWriteFlag() == null ? 0 : item.getWriteFlag());
            entity.setSort(item.getSort() == null ? 99 : item.getSort());
            entity.setRemark(item.getRemark());
            entity.setApiCode(redisIdGenerator.generateBase32IdWithPrefix("api_"));
            inserts.add(entity);
        }

        if (!inserts.isEmpty()) {
            mapper.insertBatch(inserts);
            inserts.stream().map(IamApi::getAppCode)
                .filter(code -> code != null && !code.isEmpty()).distinct()
                .forEach(this::publishAppChanged);
        }

        ApiBatchCreateResp resp = new ApiBatchCreateResp();
        resp.setSuccessCount(inserts.size());
        resp.setFailedCount(failedItems.size());
        resp.setFailedItems(failedItems);
        log.info("批量新增API完成, 成功={}, 失败={}", inserts.size(), failedItems.size());
        return resp;
    }

    /**
     * 构建批量新增失败项
     */
    private ApiBatchCreateResp.FailedItem buildFailedItem(ApiBatchCreateReq.ApiBatchCreateItemReq item, String reason) {
        ApiBatchCreateResp.FailedItem failed = new ApiBatchCreateResp.FailedItem();
        failed.setApiMethod(item.getApiMethod());
        failed.setApiUri(item.getApiUri());
        failed.setReason(reason);
        return failed;
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
            resp.setSimpleType(ClassTypeHelper.isSimpleType(node.getFieldTypeClazz()));

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
        // 唯一条件：apiCode（人工指定时校验，自动生成编码由生成器保证唯一）
        if (StringUtils.isNotBlank(entity.getApiCode())) {
            IamApi codeParam = new IamApi();
            codeParam.setApiCode(entity.getApiCode());
            codeParam = selectOneByEntity(codeParam);
            if (codeParam != null && !codeParam.getId().equals(entity.getId())) {
                // 查到有值，为新增或 id 不一样场景，为数据重复
                throw UserException.of(ResultCode.RECORD_DUPLICATE);
            }
        }

        // 唯一条件为空，直接通过
        if (StringUtils.isBlank(entity.getApiUri()) || StringUtils.isBlank(entity.getApiMethod())) {
            return;
        }

        // 唯一条件：apiMethod + apiUri 全局唯一（跨应用，支撑合并部署打平鉴权资源）
        IamApi param = new IamApi();
        param.setApiUri(entity.getApiUri());
        param.setApiMethod(entity.getApiMethod());
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

    /**
     * 导入项 method+uri 全局唯一校验：导入项之间重复 + 与库中已有记录（不同 apiCode）冲突，任一命中即拒绝
     */
    private void duplicateCheck4Import(List<ApiImportReq.ApiImportItemReq> items, List<String> errors) {
        // 库中已有 method+uri → apiCode（method 忽略大小写，与权限匹配语义一致）
        Map<String, String> existingKeyMap = new HashMap<>();
        for (IamApi api : mapper.listAllMethodUris()) {
            if (StringUtils.isNotBlank(api.getApiMethod()) && StringUtils.isNotBlank(api.getApiUri())) {
                existingKeyMap.put(api.getApiMethod().toUpperCase() + "|" + api.getApiUri(), api.getApiCode());
            }
        }
        Set<String> importKeySet = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            ApiImportReq.ApiImportItemReq item = items.get(i);
            if (StringUtils.isBlank(item.getApiMethod()) || StringUtils.isBlank(item.getApiUri())) {
                continue;
            }
            String key = item.getApiMethod().toUpperCase() + "|" + item.getApiUri();
            String tag = "第" + (i + 1) + "条";
            if (!importKeySet.add(key)) {
                errors.add(tag + "：请求方法+URI 与其他导入项重复（" + item.getApiMethod() + " " + item.getApiUri() + "）");
                continue;
            }
            String existingCode = existingKeyMap.get(key);
            if (existingCode != null && !existingCode.equals(item.getApiCode())) {
                errors.add(tag + "：请求方法+URI 已被接口 " + existingCode + " 占用（" + item.getApiMethod() + " " + item.getApiUri() + "）");
            }
        }
    }

}

