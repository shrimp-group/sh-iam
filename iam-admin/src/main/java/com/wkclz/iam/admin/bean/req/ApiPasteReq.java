package com.wkclz.iam.admin.bean.req;

import com.wkclz.iam.common.entity.IamApi;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * API粘贴请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "API粘贴请求")
public class ApiPasteReq implements Serializable {

    @NotEmpty(message = "API列表不能为空")
    @Schema(description = "待粘贴的API列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<IamApi> apis;

}
