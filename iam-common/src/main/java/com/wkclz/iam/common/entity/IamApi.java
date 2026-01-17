package com.wkclz.iam.common.entity;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;




/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_api (路由映射) 重新生成代码会覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamApi extends BaseEntity {

    /**
     * 模块
     */
    @Desc("模块")
    private String module;

    /**
     * 应用编码
     */
    @Desc("应用编码")
    private String appCode;

    /**
     * 路由映射编码
     */
    @Desc("路由映射编码")
    private String apiCode;

    /**
     * 路由映射方法
     */
    @Desc("路由映射方法")
    private String apiMethod;

    /**
     * 路由映射URI
     */
    @Desc("路由映射URI")
    private String apiUri;

    /**
     * 路由映射名称
     */
    @Desc("路由映射名称")
    private String apiName;

    /**
     * 白名单
     */
    @Desc("白名单")
    private Integer writeFlag;


    public static IamApi copy(IamApi source, IamApi target) {
        if (target == null ) { target = new IamApi();}
        if (source == null) { return target; }
        target.setId(source.getId());
        target.setModule(source.getModule());
        target.setAppCode(source.getAppCode());
        target.setApiCode(source.getApiCode());
        target.setApiMethod(source.getApiMethod());
        target.setApiUri(source.getApiUri());
        target.setApiName(source.getApiName());
        target.setWriteFlag(source.getWriteFlag());
        target.setSort(source.getSort());
        target.setCreateTime(source.getCreateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        target.setVersion(source.getVersion());
        return target;
    }

    public static IamApi copyIfNotNull(IamApi source, IamApi target) {
        if (target == null ) { target = new IamApi();}
        if (source == null) { return target; }
        if (source.getId() != null) { target.setId(source.getId()); }
        if (source.getModule() != null) { target.setModule(source.getModule()); }
        if (source.getAppCode() != null) { target.setAppCode(source.getAppCode()); }
        if (source.getApiCode() != null) { target.setApiCode(source.getApiCode()); }
        if (source.getApiMethod() != null) { target.setApiMethod(source.getApiMethod()); }
        if (source.getApiUri() != null) { target.setApiUri(source.getApiUri()); }
        if (source.getApiName() != null) { target.setApiName(source.getApiName()); }
        if (source.getWriteFlag() != null) { target.setWriteFlag(source.getWriteFlag()); }
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

