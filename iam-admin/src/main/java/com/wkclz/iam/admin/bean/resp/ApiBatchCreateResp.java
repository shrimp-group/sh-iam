package com.wkclz.iam.admin.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * API批量新增结果响应
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API批量新增结果响应")
public class ApiBatchCreateResp implements Serializable {

    @Schema(description = "成功入库数")
    private Integer successCount;

    @Schema(description = "失败数")
    private Integer failedCount;

    @Schema(description = "失败明细")
    private List<FailedItem> failedItems = new ArrayList<>();

    /**
     * 失败项明细
     */
    @Data
    @EqualsAndHashCode(callSuper = false)
    @Schema(description = "批量新增失败项")
    public static class FailedItem implements Serializable {

        @Schema(description = "请求方法")
        private String apiMethod;

        @Schema(description = "请求URI")
        private String apiUri;

        @Schema(description = "失败原因")
        private String reason;

    }

}
