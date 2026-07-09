package com.wkclz.iam.admin.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 实体字段树节点
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "实体字段树节点")
public class EntityFieldNode {

    @Schema(description = "字段名称")
    private String fieldName;

    @Schema(description = "字段描述")
    private String fieldDescription;

    @Schema(description = "字段类型")
    private String fieldType;

    @Schema(description = "字段类型Class")
    private Class<?> fieldTypeClazz;

    @Schema(description = "是否为列表类型")
    private Boolean isList;

    @Schema(description = "自动生成的JSONPath")
    private String jsonPath;

    @Schema(description = "子字段")
    private List<EntityFieldNode> children;

}
