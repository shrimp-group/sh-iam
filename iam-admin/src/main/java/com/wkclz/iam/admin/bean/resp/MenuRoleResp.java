package com.wkclz.iam.admin.bean.resp;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单-角色关系响应（用于菜单详情页展示绑定角色）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "菜单-角色关系响应")
public class MenuRoleResp extends EntityResp {

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "角色名称")
    private String roleName;

}
