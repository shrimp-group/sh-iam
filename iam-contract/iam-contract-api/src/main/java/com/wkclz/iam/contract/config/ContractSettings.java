package com.wkclz.iam.contract.config;

/**
 * 契约层静态配置持有器
 * 由 IamContractAutoConfig 在启动时通过 @PostConstruct 初始化
 * 供契约接口的 default 方法（如 AkSignContract.sign()）访问配置
 * default 方法无法访问 Spring 上下文，因此通过静态持有器桥接
 *
 * @author shrimp
 */
public final class ContractSettings {

    private static String appId;
    private static String appSecret;
    private static String publicKey;
    private static String serverUrl;
    private static String jwtSecretKey;

    private ContractSettings() {
    }


    public static String getAppId() {
        return appId;
    }

    public static void setAppId(String appId) {
        ContractSettings.appId = appId;
    }

    public static String getAppSecret() {
        return appSecret;
    }

    public static void setAppSecret(String appSecret) {
        ContractSettings.appSecret = appSecret;
    }

    public static String getPublicKey() {
        return publicKey;
    }

    public static void setPublicKey(String publicKey) {
        ContractSettings.publicKey = publicKey;
    }

    public static String getServerUrl() {
        return serverUrl;
    }

    public static void setServerUrl(String serverUrl) {
        ContractSettings.serverUrl = serverUrl;
    }

    public static String getJwtSecretKey() {
        return jwtSecretKey;
    }

    public static void setJwtSecretKey(String jwtSecretKey) {
        ContractSettings.jwtSecretKey = jwtSecretKey;
    }
}
