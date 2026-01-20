package com.wkclz.iam.common.entity;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;




/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_user_password_his (用户密码历史表) 重新生成代码会覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamUserPasswordHis extends BaseEntity {

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


    public static IamUserPasswordHis copy(IamUserPasswordHis source, IamUserPasswordHis target) {
        if (target == null ) { target = new IamUserPasswordHis();}
        if (source == null) { return target; }
        target.setId(source.getId());
        target.setUserCode(source.getUserCode());
        target.setPassword(source.getPassword());
        target.setSalt(source.getSalt());
        target.setSort(source.getSort());
        target.setCreateTime(source.getCreateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        target.setVersion(source.getVersion());
        return target;
    }

    public static IamUserPasswordHis copyIfNotNull(IamUserPasswordHis source, IamUserPasswordHis target) {
        if (target == null ) { target = new IamUserPasswordHis();}
        if (source == null) { return target; }
        if (source.getId() != null) { target.setId(source.getId()); }
        if (source.getUserCode() != null) { target.setUserCode(source.getUserCode()); }
        if (source.getPassword() != null) { target.setPassword(source.getPassword()); }
        if (source.getSalt() != null) { target.setSalt(source.getSalt()); }
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

