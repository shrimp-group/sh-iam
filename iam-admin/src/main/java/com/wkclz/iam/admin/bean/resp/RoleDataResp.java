package com.wkclz.iam.admin.bean.resp;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色数据权限关联响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "角色数据权限关联响应")
public class RoleDataResp extends EntityResp {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "数据维度编码")
    private String dimensionCode;

    @Schema(description = "数据维度值")
    private String dataCode;

}
