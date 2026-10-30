package com.wkclz.iam.admin.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * API导入结果响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API导入结果响应")
public class ApiImportResp implements Serializable {

    @Schema(description = "新增API数")
    private Long insertApiCount;

    @Schema(description = "更新API数")
    private Long updateApiCount;

    @Schema(description = "新增字段权限数")
    private Long insertFieldCount;

    @Schema(description = "更新字段权限数")
    private Long updateFieldCount;

}
