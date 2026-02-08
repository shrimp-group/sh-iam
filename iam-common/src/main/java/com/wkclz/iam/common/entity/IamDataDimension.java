package com.wkclz.iam.common.entity;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;




/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_data_dimension (数据权限维度) 重新生成代码会覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamDataDimension extends BaseEntity {

    /**
     * 应用编码
     */
    @Desc("应用编码")
    private String appCode;

    /**
     * 元数据编码
     */
    @Desc("元数据编码")
    private String dimensionCode;

    /**
     * 元数据名称
     */
    @Desc("元数据名称")
    private String dimensionName;

    /**
     * 元数据数组(优先于脚本)
     */
    @Desc("元数据数组(优先于脚本)")
    private String dimensionDataJson;

    /**
     * 元数据脚本(优先级低)
     */
    @Desc("元数据脚本(优先级低)")
    private String dimensionScript;


    public static IamDataDimension copy(IamDataDimension source, IamDataDimension target) {
        if (target == null ) { target = new IamDataDimension();}
        if (source == null) { return target; }
        target.setId(source.getId());
        target.setAppCode(source.getAppCode());
        target.setDimensionCode(source.getDimensionCode());
        target.setDimensionName(source.getDimensionName());
        target.setDimensionDataJson(source.getDimensionDataJson());
        target.setDimensionScript(source.getDimensionScript());
        target.setSort(source.getSort());
        target.setCreateTime(source.getCreateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        target.setVersion(source.getVersion());
        return target;
    }

    public static IamDataDimension copyIfNotNull(IamDataDimension source, IamDataDimension target) {
        if (target == null ) { target = new IamDataDimension();}
        if (source == null) { return target; }
        if (source.getId() != null) { target.setId(source.getId()); }
        if (source.getAppCode() != null) { target.setAppCode(source.getAppCode()); }
        if (source.getDimensionCode() != null) { target.setDimensionCode(source.getDimensionCode()); }
        if (source.getDimensionName() != null) { target.setDimensionName(source.getDimensionName()); }
        if (source.getDimensionDataJson() != null) { target.setDimensionDataJson(source.getDimensionDataJson()); }
        if (source.getDimensionScript() != null) { target.setDimensionScript(source.getDimensionScript()); }
        if (source.getSort() != null) { target.setSort(source.getSort()); }
        if (source.getCreateTime() != null) { target.setCreateTime(source.getCreateTime()); }
        if (source.getCreateBy() != null) { target.setCreateBy(source.getCreateBy()); }
        if (source.getUpdateTime() != null) { target.setUpdateTime(source.getUpdateTime()); }
        if (source.getUpdateBy() != null) { target.setUpdateBy(source.getUpdateBy()); }
        if (source.getRemark() != null) { target.setRemark(source.getRemark()); }
        if (source.getVersion() != null) { target.setVersion(source.getVersion()); }
        return target;
    }

}

