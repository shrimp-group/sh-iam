package com.wkclz.iam.admin.bean.req;

import com.wkclz.web.bean.PageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 请求日志分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "请求日志分页查询请求")
public class RequestLogPageReq extends PageReq {

    @NotNull(message = "timeFrom 不能为空")
    @Schema(description = "查询开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime timeFrom;

    @NotNull(message = "timeTo 不能为空")
    @Schema(description = "查询结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime timeTo;

}
