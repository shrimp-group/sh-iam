package com.wkclz.iam.portal.bean.req;

import com.wkclz.web.bean.PageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户选择分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户选择分页查询请求")
public class UserSelectReq extends PageReq {

    @Schema(description = "用户名（模糊匹配）")
    private String username;

    @Schema(description = "姓名（模糊匹配）")
    private String nickname;

    @Schema(description = "性别（精确匹配）")
    private Integer gender;

}
