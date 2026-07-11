package com.wkclz.iam.contract.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 字段权限
 *
 * @author shrimp
 */
@Data
@Schema(description = "字段权限")
public class FieldPermission implements Serializable {

    @Schema(description = "字段编码")
    private String fieldCode;

    @Schema(description = "字段名称")
    private String fieldName;

    @Schema(description = "是否可见")
    private Boolean visible;

    @Schema(description = "是否可编辑")
    private Boolean editable;
}
