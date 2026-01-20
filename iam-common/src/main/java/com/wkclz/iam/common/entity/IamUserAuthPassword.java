package com.wkclz.iam.common.entity;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;


/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_user_auth_password (密码认证表) 重新生成代码会覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamUserAuthPassword extends BaseEntity {

    /**
     * 用户编码
     */
    @Desc("用户编码")
    private String userCode;

    /**
     * 加密后的密码
     */
    @Desc("加密后的密码")
    private String password;

    /**
     * 密码盐值
     */
    @Desc("密码盐值")
    private String salt;

    /**
     * 最后修改时间
     */
    @Desc("最后修改时间")
    private LocalDateTime lastChangedTime;


    public static IamUserAuthPassword copy(IamUserAuthPassword source, IamUserAuthPassword target) {
        if (target == null ) { target = new IamUserAuthPassword();}
        if (source == null) { return target; }
        target.setId(source.getId());
        target.setUserCode(source.getUserCode());
        target.setPassword(source.getPassword());
        target.setSalt(source.getSalt());
        target.setLastChangedTime(source.getLastChangedTime());
        target.setSort(source.getSort());
        target.setCreateTime(source.getCreateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        target.setVersion(source.getVersion());
        return target;
    }

    public static IamUserAuthPassword copyIfNotNull(IamUserAuthPassword source, IamUserAuthPassword target) {
        if (target == null ) { target = new IamUserAuthPassword();}
        if (source == null) { return target; }
        if (source.getId() != null) { target.setId(source.getId()); }
        if (source.getUserCode() != null) { target.setUserCode(source.getUserCode()); }
        if (source.getPassword() != null) { target.setPassword(source.getPassword()); }
        if (source.getSalt() != null) { target.setSalt(source.getSalt()); }
        if (source.getLastChangedTime() != null) { target.setLastChangedTime(source.getLastChangedTime()); }
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

