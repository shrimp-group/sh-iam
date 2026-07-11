package com.wkclz.iam.sdk.contract.service;

import com.wkclz.iam.sdk.contract.config.ContractSettings;
import com.wkclz.iam.sdk.contract.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * AK 签名契约
 * 用于服务间 RPC 调用的签名认证
 *
 * @author shrimp
 */
public interface AkSignContract {

    /**
     * 生成 AK 签名（客户端调用，用于服务间 RPC 请求）
     *
     * @param appId     应用 ID
     * @param appSecret 应用密钥（RSA 私钥）
     * @return 签名字符串，放入请求头 sign 字段
     */
    String sign(String appId, String appSecret);

    /**
     * 生成 AK 签名（重载：从 ContractSettings 获取 appId + appSecret）
     */
    default String sign() {
        return sign(ContractSettings.getAppId(), ContractSettings.getAppSecret());
    }

    /**
     * 验证 AK 签名（服务端调用）
     * <p>
     * 实现职责：
     * 1. RSA 公钥解密签名，解析参数（appId / nonce / timestamp）
     * 2. 校验签名中的 appId 与请求头 app-id 一致
     * 3. 校验 timestamp 在 5 分钟有效期内
     * 4. nonce 防重放校验（如 Redis SETNX）
     *
     * @param sign          请求头中的签名
     * @param publicKey     服务端配置的 RSA 公钥
     * @param expectedAppId 请求头中的 app-id（与签名内容比对）
     * @return 验签通过返回 true
     * @throws AuthException 验签失败（appId 不匹配 / 签名过期 / 重放检测）
     */
    boolean verifySign(String sign, String publicKey, String expectedAppId);

    /**
     * 验证 AK 签名（重载：从请求头 + ContractSettings 自动获取参数）
     */
    default boolean verifySign(HttpServletRequest request) {
        String sign = request.getHeader("sign");
        String appId = request.getHeader("app-id");
        return verifySign(sign, ContractSettings.getPublicKey(), appId);
    }
}
