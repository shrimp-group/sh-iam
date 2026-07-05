package com.wkclz.iam.admin.bean.resp;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 角色响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "角色响应")
public class RoleResp extends EntityResp {

    @Schema(description = "租户编码")
    private String tenantCode;

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "父角色编码")
    private String parentCode;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "是否可申请")
    private Integer applicable;

    @Schema(description = "子角色列表")
    private List<RoleResp> children;

}
