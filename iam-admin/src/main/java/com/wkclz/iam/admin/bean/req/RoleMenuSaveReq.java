package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 角色-菜单保存请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "角色-菜单保存请求")
public class RoleMenuSaveReq {

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "菜单编码列表")
    private List<String> menuCodes;

}
