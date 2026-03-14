package com.wkclz.iam.common.entity;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;




/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_access_key_api (AK 接口) 重新生成代码会覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamAccessKeyApi extends BaseEntity {

    /**
     * 所属应用
     */
    @Desc("所属应用")
    private String appCode;

    /**
     * 应用id
     */
    @Desc("应用id")
    private String appId;

    /**
     * AK
     */
    @Desc("AK")
    private String apiId;


    public static IamAccessKeyApi copy(IamAccessKeyApi source, IamAccessKeyApi target) {
        if (target == null ) { target = new IamAccessKeyApi();}
        if (source == null) { return target; }
        target.setId(source.getId());
        target.setAppCode(source.getAppCode());
        target.setAppId(source.getAppId());
        target.setApiId(source.getApiId());
        target.setSort(source.getSort());
        target.setCreateTime(source.getCreateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        target.setVersion(source.getVersion());
        return target;
    }

    public static IamAccessKeyApi copyIfNotNull(IamAccessKeyApi source, IamAccessKeyApi target) {
        if (target == null ) { target = new IamAccessKeyApi();}
        if (source == null) { return target; }
        if (source.getId() != null) { target.setId(source.getId()); }
        if (source.getAppCode() != null) { target.setAppCode(source.getAppCode()); }
        if (source.getAppId() != null) { target.setAppId(source.getAppId()); }
        if (source.getApiId() != null) { target.setApiId(source.getApiId()); }
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

