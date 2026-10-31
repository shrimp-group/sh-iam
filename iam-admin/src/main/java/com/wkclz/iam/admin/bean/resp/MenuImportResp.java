package com.wkclz.iam.admin.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单导入结果响应：新增/更新菜单数、重建绑定数与 parentCode 修复提示
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "菜单导入结果响应")
public class MenuImportResp implements Serializable {

    @Schema(description = "新增菜单数")
    private Long insertMenuCount;

    @Schema(description = "更新菜单数")
    private Long updateMenuCount;

    @Schema(description = "重建绑定关系数")
    private Long rebuildBindCount;

    @Schema(description = "父级不存在已置为顶级的提示列表")
    private List<String> parentFixHints;

}
