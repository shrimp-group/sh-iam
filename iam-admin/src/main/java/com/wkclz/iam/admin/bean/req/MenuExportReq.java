package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单导出请求：ids 有值时导出选中节点及其全部子孙（内存沿 parentCode 递归补齐），否则按 appCode 全量导出
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "菜单导出请求")
public class MenuExportReq implements Serializable {

    @Schema(description = "主键ID列表（导出选中节点及子孙时使用，优先于应用筛选）")
    private List<Long> ids;

    @Schema(description = "应用编码")
    private String appCode;

}
