package com.wkclz.iam.common.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_api_field (API字段权限) 重新生成代码会覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamApiField extends BaseEntity {

    /**
     * 应用编码
     */
    @Schema(description = "应用编码")
    private String appCode;

    /**
     * API编码
     */
    @Schema(description = "API编码")
    private String apiCode;

    /**
     * 字段编码
     */
    @Schema(description = "字段编码")
    private String fieldCode;

    /**
     * 字段名称
     */
    @Schema(description = "字段名称")
    private String fieldName;

    /**
     * JSONPath表达式
     */
    @Schema(description = "JSONPath表达式")
    private String jsonPath;

    /**
     * 动作类型(HIDDEN/MASK/READ_ONLY)
     */
    @Schema(description = "动作类型(HIDDEN/MASK/READ_ONLY)")
    private String action;

    /**
     * 脱敏规则(keepHead,keepTail)
     */
    @Schema(description = "脱敏规则(keepHead,keepTail)")
    private String maskRule;

    /**
     * 描述说明
     */
    @Schema(description = "描述说明")
    private String description;


    public static IamApiField copy(IamApiField source, IamApiField target) {
        if (target == null) {
            target = new IamApiField();
        }
        if (source == null) {
            return target;
        }
        target.setId(source.getId());
        target.setAppCode(source.getAppCode());
        target.setApiCode(source.getApiCode());
        target.setFieldCode(source.getFieldCode());
        target.setFieldName(source.getFieldName());
        target.setJsonPath(source.getJsonPath());
        target.setAction(source.getAction());
        target.setMaskRule(source.getMaskRule());
        target.setDescription(source.getDescription());
        target.setSort(source.getSort());
        target.setCreateTime(source.getCreateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        target.setVersion(source.getVersion());
        return target;
    }

    public static IamApiField copyIfNotNull(IamApiField source, IamApiField target) {
        if (target == null) {
            target = new IamApiField();
        }
        if (source == null) {
            return target;
        }
        if (source.getId() != null) {
            target.setId(source.getId());
        }
        if (source.getAppCode() != null) {
            target.setAppCode(source.getAppCode());
        }
        if (source.getApiCode() != null) {
            target.setApiCode(source.getApiCode());
        }
        if (source.getFieldCode() != null) {
            target.setFieldCode(source.getFieldCode());
        }
        if (source.getFieldName() != null) {
            target.setFieldName(source.getFieldName());
        }
        if (source.getJsonPath() != null) {
            target.setJsonPath(source.getJsonPath());
        }
        if (source.getAction() != null) {
            target.setAction(source.getAction());
        }
        if (source.getMaskRule() != null) {
            target.setMaskRule(source.getMaskRule());
        }
        if (source.getDescription() != null) {
            target.setDescription(source.getDescription());
        }
        if (source.getSort() != null) {
            target.setSort(source.getSort());
        }
        if (source.getCreateTime() != null) {
            target.setCreateTime(source.getCreateTime());
        }
        if (source.getCreateBy() != null) {
            target.setCreateBy(source.getCreateBy());
        }
        if (source.getUpdateTime() != null) {
            target.setUpdateTime(source.getUpdateTime());
        }
        if (source.getUpdateBy() != null) {
            target.setUpdateBy(source.getUpdateBy());
        }
        if (source.getRemark() != null) {
            target.setRemark(source.getRemark());
        }
        if (source.getVersion() != null) {
            target.setVersion(source.getVersion());
        }
        return target;
    }

}
