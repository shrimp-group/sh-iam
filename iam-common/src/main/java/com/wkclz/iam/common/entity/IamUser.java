package com.wkclz.iam.common.entity;

import com.wkclz.core.annotation.Desc;
import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;




/**
 * Description Create by sh-generator
 * @author shrimp
 * @table iam_user (用户表) 重新生成代码会覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamUser extends BaseEntity {

    /**
     * 用户编码
     */
    @Desc("用户编码")
    private String userCode;

    /**
     * 用户名
     */
    @Desc("用户名")
    private String username;

    /**
     * 昵称
     */
    @Desc("昵称")
    private String nickname;

    /**
     * 邮箱
     */
    @Desc("邮箱")
    private String email;

    /**
     * 手机号
     */
    @Desc("手机号")
    private String phone;

    /**
     * 头像
     */
    @Desc("头像")
    private String avatar;

    /**
     * 状态：1-启用，2-禁用，3-锁定
     */
    @Desc("状态：1-启用，2-禁用，3-锁定")
    private Integer userStatus;


    public static IamUser copy(IamUser source, IamUser target) {
        if (target == null ) { target = new IamUser();}
        if (source == null) { return target; }
        target.setId(source.getId());
        target.setUserCode(source.getUserCode());
        target.setUsername(source.getUsername());
        target.setNickname(source.getNickname());
        target.setEmail(source.getEmail());
        target.setPhone(source.getPhone());
        target.setAvatar(source.getAvatar());
        target.setUserStatus(source.getUserStatus());
        target.setSort(source.getSort());
        target.setCreateTime(source.getCreateTime());
        target.setCreateBy(source.getCreateBy());
        target.setUpdateTime(source.getUpdateTime());
        target.setUpdateBy(source.getUpdateBy());
        target.setRemark(source.getRemark());
        target.setVersion(source.getVersion());
        return target;
    }

    public static IamUser copyIfNotNull(IamUser source, IamUser target) {
        if (target == null ) { target = new IamUser();}
        if (source == null) { return target; }
        if (source.getId() != null) { target.setId(source.getId()); }
        if (source.getUserCode() != null) { target.setUserCode(source.getUserCode()); }
        if (source.getUsername() != null) { target.setUsername(source.getUsername()); }
        if (source.getNickname() != null) { target.setNickname(source.getNickname()); }
        if (source.getEmail() != null) { target.setEmail(source.getEmail()); }
        if (source.getPhone() != null) { target.setPhone(source.getPhone()); }
        if (source.getAvatar() != null) { target.setAvatar(source.getAvatar()); }
        if (source.getUserStatus() != null) { target.setUserStatus(source.getUserStatus()); }
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

