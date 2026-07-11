package com.wkclz.iam.contract.bean.resp;

import com.wkclz.iam.contract.enums.LoginFailType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应
 * 由 SsoFacadeContract.login() 返回
 * 成功时 success=true，失败字段为 null；失败时 success=false，成功字段为 null
 *
 * @author shrimp
 */
@Data
@Schema(description = "登录响应")
public class LoginResp implements Serializable {

    @Schema(description = "是否登录成功")
    private Boolean success;

    @Schema(description = "登录失败类型；成功时为 null")
    private LoginFailType failType;

    @Schema(description = "登录失败动态详情；成功时为 null")
    private String failReason;

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    /**
     * 构造登录成功响应
     * success=true，failType/failReason 为 null
     */
    public static LoginResp success(String token, String userCode, String username, String nickname, String avatar) {
        LoginResp resp = new LoginResp();
        resp.setSuccess(true);
        resp.setToken(token);
        resp.setUserCode(userCode);
        resp.setUsername(username);
        resp.setNickname(nickname);
        resp.setAvatar(avatar);
        return resp;
    }

    /**
     * 构造登录失败响应（无动态详情）
     * success=false，failReason 为 null
     */
    public static LoginResp fail(LoginFailType failType) {
        LoginResp resp = new LoginResp();
        resp.setSuccess(false);
        resp.setFailType(failType);
        return resp;
    }

    /**
     * 构造登录失败响应（含动态详情）
     * success=false，failReason 为运行时补充（如"请 300 秒后重试"）
     */
    public static LoginResp fail(LoginFailType failType, String failReason) {
        LoginResp resp = new LoginResp();
        resp.setSuccess(false);
        resp.setFailType(failType);
        resp.setFailReason(failReason);
        return resp;
    }
}
