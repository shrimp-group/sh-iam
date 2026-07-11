package com.wkclz.iam.contract.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 数据权限维度
 * authorizedValues 为通用值列表，业务层根据 dimensionCode 解释含义
 *
 * @author shrimp
 */
@Data
@Schema(description = "数据权限维度")
public class DataDimension implements Serializable {

    @Schema(description = "维度编码")
    private String dimensionCode;

    @Schema(description = "维度名称")
    private String dimensionName;

    @Schema(description = "授权值列表（如部门 ID、区域编码等）")
    private List<String> authorizedValues;
}
