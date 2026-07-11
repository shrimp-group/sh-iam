package com.wkclz.iam.contract.defaults.service;

import com.wkclz.iam.contract.enums.AuthErrorType;
import com.wkclz.iam.contract.exception.AuthException;
import com.wkclz.iam.contract.service.AkSignContract;
import lombok.extern.slf4j.Slf4j;

/**
 * AK 签名契约默认实现
 * 功能不可用：sign/verifySign 均抛异常
 * AK 签名是功能性操作，没有实现就不该被调用，抛异常比静默更安全
 *
 * @author shrimp
 */
@Slf4j
public class DefaultAkSignContract implements AkSignContract {

    @Override
    public String sign(String appId, String appSecret) {
        log.warn("DefaultAkSignContract: sign 无实现");
        throw new UnsupportedOperationException("无 AK 签名实现，请配置 AkSignContract");
    }

    @Override
    public boolean verifySign(String sign, String publicKey, String expectedAppId) {
        log.warn("DefaultAkSignContract: verifySign 无实现");
        throw new AuthException(AuthErrorType.AK_SIGN_INVALID,
                "无 AK 签名实现，请配置 AkSignContract");
    }
}
