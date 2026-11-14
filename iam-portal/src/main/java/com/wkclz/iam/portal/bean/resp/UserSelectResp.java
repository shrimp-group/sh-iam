package com.wkclz.iam.portal.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户选择分页查询响应
 */
@Data
@Schema(description = "用户选择分页查询响应")
public class UserSelectResp {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "姓名")
    private String nickname;

    @Schema(description = "性别")
    private Integer gender;

}
