package com.wkclz.iam.admin.bean.resp;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色绑定信息响应（用于菜单详情页展示绑定角色）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "角色绑定信息响应")
public class RoleBoundResp extends EntityResp {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "是否可申请")
    private Integer applicable;

}
