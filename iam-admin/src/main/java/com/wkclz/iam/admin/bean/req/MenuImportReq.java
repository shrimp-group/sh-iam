package com.wkclz.iam.admin.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单导入请求：按 menuCode 判断存在性，存在更新、不存在新增；对显式携带 apiCodes 的菜单做绑定全量快照重建。
 * 节点内字段校验在 Service 内存逻辑完成（错误信息带条目位置），apiCodes 与 children 不做注解级联必填
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "菜单导入请求")
public class MenuImportReq implements Serializable {

    @NotEmpty(message = "菜单列表不能为空")
    @Valid
    @Schema(description = "待导入的菜单树列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<MenuNode> menus;

    /**
     * 菜单导入节点，字段结构同 MenuExportResp（children 为 MenuNode 递归）
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    @Schema(description = "菜单导入节点")
    public static class MenuNode implements Serializable {

        @Schema(description = "应用编码")
        private String appCode;

        @Schema(description = "父编码，顶级为0")
        private String parentCode;

        @Schema(description = "菜单编码")
        private String menuCode;

        @Schema(description = "菜单名称")
        private String menuName;

        @Schema(description = "图标")
        private String icon;

        @Schema(description = "菜单类型:菜单MENU, 按钮BUTTON, 字段组FIELDS")
        private String menuType;

        @Schema(description = "路由地址")
        private String routePath;

        @Schema(description = "组件")
        private String component;

        @Schema(description = "权限标识符")
        private String buttonCode;

        @Schema(description = "隐藏：0-否，1-是")
        private Integer hidden;

        @Schema(description = "备注")
        private String remark;

        @Schema(description = "排序")
        private Integer sort;

        @Schema(description = "绑定的接口编码列表；字段缺失则不重建该菜单的现有绑定，空数组则仅清空绑定")
        private List<String> apiCodes;

        @Schema(description = "子菜单，递归结构同父节点")
        private List<MenuNode> children;

    }

}
