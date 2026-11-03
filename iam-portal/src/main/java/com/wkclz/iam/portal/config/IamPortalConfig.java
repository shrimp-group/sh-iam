package com.wkclz.iam.portal.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author shrimp
 */
@Data
@Configuration
public class IamPortalConfig {

    @Value("${iam.login.public-key:}")
    private String publicKey;
    @Value("${iam.login.private-key:}")
    private String privateKey;


    @Value("${iam.portal.password.expire-days:180}")
    private Integer passwordExpireDays;

    @Value("${iam.portal.max-concurrent-sessions:0}")
    private Integer maxConcurrentSessions;

    @Value("${iam.portal.password-history-size:5}")
    private Integer passwordHistorySize;

    /**
     * 权限资源远程取数鉴权 token（简单共享密钥；密钥对方式后续重新设计）
     */
    @Value("${iam.portal.auth.token:}")
    private String authToken;

    /**
     * super 用户编码列表（逗号分隔），配置内用户获取角色时返回哨兵角色，视为拥有应用内全部角色
     */
    @Value("${iam.portal.super-users:root}")
    private List<String> superUsers;

}
