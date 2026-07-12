package com.wkclz.iam.sdk.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 验证码响应对象
 *
 * @author shrimp
 */
/**
 * @deprecated 已由 sh-auth Captcha bean 替代
 */
@Data
@Deprecated
@Schema(description = "图形验证码响应")
public class PictureCaptchaResp implements Serializable {

    @Schema(description = "验证码")
    private String captchaCode;

    @Schema(description = "验证码ID")
    private String captchaId;

    @Schema(description = "验证码图片(Base64)")
    private String captchaImage;

    @Schema(description = "过期时间")
    private long expireTime;

}
