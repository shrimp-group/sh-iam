package com.wkclz.iam.sso.spi;

import com.wkclz.iam.sso.service.CredentialCheckResult;

/**
 * 凭证校验 SPI — 根据用户名和原始密码校验凭证。
 */
public interface CredentialChecker {

    /**
     * 校验凭证。
     *
     * @param username    用户名
     * @param rawPassword 原始密码（已解密）
     * @return 校验结果
     */
    CredentialCheckResult check(String username, String rawPassword);

}
