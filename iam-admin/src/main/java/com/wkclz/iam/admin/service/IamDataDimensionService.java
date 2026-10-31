package com.wkclz.iam.admin.service;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.wkclz.core.base.PageData;
import com.wkclz.core.enums.ResultCode;
import com.wkclz.core.exception.UserException;
import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.admin.bean.req.DataDimExportReq;
import com.wkclz.iam.admin.bean.req.DataDimImportReq;
import com.wkclz.iam.admin.bean.resp.DataDimExportResp;
import com.wkclz.iam.admin.bean.resp.DataDimImportResp;
import com.wkclz.iam.admin.mapper.IamDataDimensionMapper;
import com.wkclz.iam.common.entity.IamDataDimension;
import com.wkclz.mybatis.helper.PageQuery;
import com.wkclz.mybatis.service.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_data_dimension (数据权限维度) 单表服务类，代码重新生成不覆盖. 只建议完成单表的逻辑，或主表为 iam_data_dimension 的逻辑. 其他逻辑放 custom 中
 */

@Slf4j
@Service
public class IamDataDimensionService extends BaseService<IamDataDimension, IamDataDimensionMapper> {

    /**
     * 导入批量分块大小，避免单 SQL 过大
     */
    private static final int IMPORT_BATCH_SIZE = 1000;

    public PageData<IamDataDimension> getDataDimensionPage(IamDataDimension entity) {
        return PageQuery.page(entity, mapper::getDataDimensionList);
    }

    /**
     * 数据维度导出：ids 有值时按主键批量导出（选中项），否则按筛选条件全量导出（与页面查询同语义，无分页）
     *
     * @param req 导出请求
     * @return 导出数据（仅业务字段）
     */
    public List<DataDimExportResp> getDataDims4Export(DataDimExportReq req) {
        log.info("导出数据维度, ids={}, appCode={}", req.getIds(), req.getAppCode());

        // 主数据一次批量查询：选中项按主键，全量按筛选条件
        List<IamDataDimension> dims;
        if (!CollectionUtils.isEmpty(req.getIds())) {
            dims = mapper.selectByIds(req.getIds());
        } else {
            IamDataDimension param = new IamDataDimension();
            param.setAppCode(req.getAppCode());
            param.setDimensionCode(req.getDimensionCode());
            param.setDimensionName(req.getDimensionName());
            dims = mapper.getDims4Export(param);
        }
        if (dims.isEmpty()) {
            return Collections.emptyList();
        }

        // 循环仅内存转换
        List<DataDimExportResp> respList = new ArrayList<>(dims.size());
        for (IamDataDimension dim : dims) {
            respList.add(buildExportResp(dim));
        }
        log.info("导出数据维度完成, 维度数={}", respList.size());
        return respList;
    }

    /**
     * 数据维度导入：按 dimensionCode 判断存在性，存在更新、不存在新增，全程无删除
     *
     * @param req 导入请求
     * @return 新增/更新条数明细
     */
    @Transactional(rollbackFor = Exception.class)
    public DataDimImportResp dataDimImport(DataDimImportReq req) {
        log.info("导入数据维度, 条数={}", req.getDims().size());

        // 内存校验全部通过后才落库
        validateImport(req.getDims());

        // 一次 IN 批量查询已存在数据，内存划分新增/更新
        List<String> dimensionCodes = req.getDims().stream().map(DataDimImportReq.DataDimImportItemReq::getDimensionCode).toList();
        Map<String, IamDataDimension> existingMap = mapper.getDimsByCodes(dimensionCodes).stream()
            .collect(Collectors.toMap(IamDataDimension::getDimensionCode, t -> t, (a, b) -> a));

        List<IamDataDimension> inserts = new ArrayList<>();
        List<IamDataDimension> updates = new ArrayList<>();
        for (DataDimImportReq.DataDimImportItemReq item : req.getDims()) {
            IamDataDimension existing = existingMap.get(item.getDimensionCode());
            IamDataDimension entity = buildImportEntity(item);
            if (existing == null) {
                inserts.add(entity);
            } else {
                entity.setId(existing.getId());
                updates.add(entity);
            }
        }

        // 分块批量落库，避免单 SQL 过大
        for (List<IamDataDimension> chunk : Lists.partition(inserts, IMPORT_BATCH_SIZE)) {
            mapper.insertBatch(chunk);
        }
        for (List<IamDataDimension> chunk : Lists.partition(updates, IMPORT_BATCH_SIZE)) {
            mapper.updateBatch4Import(chunk);
        }

        DataDimImportResp resp = new DataDimImportResp();
        resp.setInsertCount((long) inserts.size());
        resp.setUpdateCount((long) updates.size());
        log.info("导入数据维度完成, 新增={}, 更新={}", resp.getInsertCount(), resp.getUpdateCount());
        return resp;
    }

    /**
     * 导入数据校验：逐条定位缺失项，任一不合法则整体不通过
     */
    private void validateImport(List<DataDimImportReq.DataDimImportItemReq> items) {
        List<String> errors = new ArrayList<>();
        Set<String> dimensionCodeSet = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            DataDimImportReq.DataDimImportItemReq item = items.get(i);
            String tag = "第" + (i + 1) + "项(" + (StringUtils.isBlank(item.getDimensionCode()) ? "?" : item.getDimensionCode()) + ")";
            if (StringUtils.isBlank(item.getAppCode())) {
                errors.add(tag + "：缺少应用编码");
            }
            if (StringUtils.isBlank(item.getDimensionCode())) {
                errors.add(tag + "：缺少维度编码");
            }
            if (StringUtils.isBlank(item.getDimensionName())) {
                errors.add(tag + "：缺少维度名称");
            }
            if (StringUtils.isNotBlank(item.getDimensionCode()) && !dimensionCodeSet.add(item.getDimensionCode())) {
                errors.add(tag + "：维度编码 " + item.getDimensionCode() + " 重复出现");
            }
            if (StringUtils.isNotBlank(item.getDimensionDataJson()) && !isLegalJson(item.getDimensionDataJson())) {
                errors.add(tag + "：维度数据JSON不合法");
            }
        }
        if (!errors.isEmpty()) {
            log.warn("导入数据维度校验失败, 错误数={}", errors.size());
            throw ValidationException.of(String.join("；", errors));
        }
    }

    private DataDimExportResp buildExportResp(IamDataDimension dim) {
        DataDimExportResp resp = new DataDimExportResp();
        resp.setAppCode(dim.getAppCode());
        resp.setDimensionCode(dim.getDimensionCode());
        resp.setDimensionName(dim.getDimensionName());
        resp.setDimensionDataJson(dim.getDimensionDataJson());
        resp.setDimensionScript(dim.getDimensionScript());
        resp.setRemark(dim.getRemark());
        resp.setSort(dim.getSort());
        return resp;
    }

    private IamDataDimension buildImportEntity(DataDimImportReq.DataDimImportItemReq item) {
        IamDataDimension entity = new IamDataDimension();
        entity.setAppCode(item.getAppCode());
        entity.setDimensionCode(item.getDimensionCode());
        entity.setDimensionName(item.getDimensionName());
        entity.setDimensionDataJson(item.getDimensionDataJson());
        entity.setDimensionScript(item.getDimensionScript());
        entity.setRemark(item.getRemark());
        entity.setSort(item.getSort());
        return entity;
    }

    /**
     * JSON 字符串合法性校验：可解析即合法
     *
     * @param json JSON 字符串
     * @return true-合法
     */
    private boolean isLegalJson(String json) {
        try {
            JSON.parse(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public IamDataDimension create(IamDataDimension entity) {
        duplicateCheck(entity);
        mapper.insert(entity);
        return entity;
    }

    public IamDataDimension update(IamDataDimension entity) {
        duplicateCheck(entity);
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        Assert.notNull(entity.getVersion(), "请求错误！参数[version]不能为空");
        IamDataDimension oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        IamDataDimension.copyIfNotNull(entity, oldEntity);
        updateByIdSelective(oldEntity);
        return oldEntity;
    }

    public IamDataDimension save(IamDataDimension entity) {
        return entity.getId() == null ? create(entity) : update(entity);
    }

    public IamDataDimension remove(IamDataDimension entity) {
        Assert.notNull(entity.getId(), "请求错误！参数[id]不能为空");
        IamDataDimension oldEntity = selectById(entity.getId());
        if (oldEntity == null) {
            throw ValidationException.of(ResultCode.RECORD_NOT_EXIST);
        }
        deleteById(oldEntity);
        return oldEntity;
    }

    private void duplicateCheck(IamDataDimension entity) {
        // 唯一条件为空，直接通过
        if (StringUtils.isBlank(entity.getDimensionCode())) {
            return;
        }

        // 唯一条件不为空，请设置唯一条件
        IamDataDimension param = new IamDataDimension();
        // 唯一条件
        param.setDimensionCode(entity.getDimensionCode());
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
