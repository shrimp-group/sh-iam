package com.wkclz.iam.common.dto;

import com.wkclz.iam.common.entity.IamUserRole;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Description Create by sh-generator
 * @author shrimp
 * @table IamUserRole (用户-角色关系) 数据库实例扩展，代码重新生成不覆盖
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IamUserRoleDto extends IamUserRole {

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 父角色编码（用于角色树构建）
     */
    private String parentCode;

    /**
     * 绑定数量（用于角色树展示）
     */
    private Integer bindCount;

    /**
     * 子角色列表（用于角色树）
     */
    private List<IamUserRoleDto> children;

    /**
     * entity 转 Dto
     * @param source
     * @return
     */
    public static IamUserRoleDto copy(IamUserRole source) {
        IamUserRoleDto target = new IamUserRoleDto();
        IamUserRole.copy(source, target);
        return target;
    }
}

