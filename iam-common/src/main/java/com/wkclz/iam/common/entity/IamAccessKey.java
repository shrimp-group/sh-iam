package com.wkclz.iam.common.entity;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;


/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_access_key (AK 密钥) 重新生成代码会覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamAccessKey extends BaseEntity {

    /**
     * 应用编码
     */
    @Desc("应用编码")
    private String appCode;

    /**
     * 模块
     */
    @Desc("模块")
    private String accessKeyId;

    /**
     * 应用编码
     */
    @Desc("应用编码")
    private String accessKey;

    /**
     * 路由映射编码
     */
    @Desc("路由映射编码")
    private String secretKey;

    /**
     * 生效状态
     */
    @Desc("生效状态")
    private Integer enableStatus;

    /**
     * 生效时间开始
     */
    @Desc("生效时间开始")
    private LocalDateTime enableStart;

    /**
     * 生效时间结束
     */
    @Desc("生效时间结束")
    private LocalDateTime enableStop;


    public static IamAccessKey copy(IamAccessKey source, IamAccessKey target) {
        if (target == null ) { target = new IamAccessKey();}
        if (source == null) { return target; }
        target.setId(source.getId());
        target.setAppCode(source.getAppCode());
        target.setAccessKeyId(source.getAccessKeyId());
        target.setAccessKey(source.getAccessKey());
        target.setSecretKey(source.getSecretKey());
        target.setEnableStatus(source.getEnableStatus());
        target.setEnableStart(source.getEnableStart());
        target.setEnableStop(source.getEnableStop());
        target.setSort(source.getSort());
        target.setCreateTime(source.getCreateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        target.setVersion(source.getVersion());
        return target;
    }

    public static IamAccessKey copyIfNotNull(IamAccessKey source, IamAccessKey target) {
        if (target == null ) { target = new IamAccessKey();}
        if (source == null) { return target; }
        if (source.getId() != null) { target.setId(source.getId()); }
        if (source.getAppCode() != null) { target.setAppCode(source.getAppCode()); }
        if (source.getAccessKeyId() != null) { target.setAccessKeyId(source.getAccessKeyId()); }
        if (source.getAccessKey() != null) { target.setAccessKey(source.getAccessKey()); }
        if (source.getSecretKey() != null) { target.setSecretKey(source.getSecretKey()); }
        if (source.getEnableStatus() != null) { target.setEnableStatus(source.getEnableStatus()); }
        if (source.getEnableStart() != null) { target.setEnableStart(source.getEnableStart()); }
        if (source.getEnableStop() != null) { target.setEnableStop(source.getEnableStop()); }
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

