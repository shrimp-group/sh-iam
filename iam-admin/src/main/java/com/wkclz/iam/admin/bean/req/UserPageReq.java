package com.wkclz.iam.admin.bean.req;

import com.wkclz.web.bean.PageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Schema(description = "用户分页查询请求")
public class UserPageReq extends PageReq {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "状态：1-启用，2-禁用，3-锁定")
    private Integer userStatus;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

}
