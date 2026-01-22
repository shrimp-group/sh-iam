package com.wkclz.iam.sdk.helper;

import com.wkclz.core.exception.ValidationException;
import com.wkclz.iam.sdk.config.IamSdkConfig;
import com.wkclz.tool.tools.Md5Tool;
import com.wkclz.tool.tools.RsaTool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;


/**
 * @author shrimp
 */
@Slf4j
@Component
public class AkSignHelper {


    @Resource
    private IamSdkConfig config;
//    @Resource
//    private RedisLock redisLock;


    /**
     * 获取签名
     */
    public static String sign(String appId, String appSecret) {
        if (StringUtils.isBlank(appId)) {
            throw ValidationException.of("appId 不能为空");
        }
        if (StringUtils.isBlank(appSecret)) {
            throw ValidationException.of("appSecret 不能为空");
        }
        long timestamp = System.currentTimeMillis();
        String nonce = Md5Tool.md5(UUID.randomUUID().toString() + timestamp);

        // 生成签名
        Map<String, String> data = new HashMap<>();
        data.put("appId", appId);
        data.put("nonce", nonce);
        data.put("timestamp", timestamp + "");
        Set<String> keySet = data.keySet();
        String[] keyArray = keySet.toArray(new String[keySet.size()]);
        // 排序
        Arrays.sort(keyArray);
        StringBuilder sb = new StringBuilder();
        for (String k : keyArray) {
            // 参数值为空，则不参与签名
            if (!data.get(k).trim().isEmpty()) {
                sb.append(k).append("=").append(data.get(k).trim()).append("&");
            }
        }
        return RsaTool.encryptByPrivateKey(sb.substring(0, sb.length() - 1), appSecret);
    }




    /**
     * 解析并校验签名
     */
    /*
    public boolean deSign(String appId, String sign) {
        if (StringUtils.isBlank(appId) || StringUtils.isBlank(sign)) {
            return false;
        }
        String appCode = config.getAppCode();
        if (StringUtils.isBlank(appCode)) {
            log.error("appCode: 未配置，无法使用签名调用");
            return false;
        }

        AppInfo appInfo = appInfoCache.get(appCode);
        if (appInfo == null) {
            log.error("appCode 错误，应用信息： {}", appCode);
            return false;
        }
        Map<String, AccessToken> accessTokens = appInfo.getAccessTokens();
        if (accessTokens == null || accessTokens.isEmpty()) {
            log.error("{} 下没有accessToken 信息，无法使用签名调用", appCode);
            return false;
        }
        AccessToken accessToken = accessTokens.get(appId);
        if (accessToken == null) {
            log.error("appId: {} 不存在, 无法解密", appId);
            return false;
        }

        // 签名验证
        String decrypt;
        try {
            decrypt = RsaTool.decryptByPublicKey(sign, accessToken.getAppPublicKey());
        } catch (Exception e) {
            log.error("appId: {} 解密失败", appId);
            return false;
        }

        // 获取信息进行详情验证
        String[] split = decrypt.split("&");
        Map<String, String> data = new HashMap<>();
        for (String s : split) {
            String[] t = s.split("=");
            if (t.length == 2) {
                data.put(t[0], t[1]);
            }
        }

        String appId2 = data.get("appId");
        String timestamp = data.get("timestamp");
        String nonce = data.get("nonce");

        // 验证 appId1 是否被偷换
        if (appId2 == null) {
            log.error("签名中缺少 appId: {}! ", appId);
            return false;
        }
        if (!appId2.equals(appId)) {
            log.error("请求appId: {}, 验证appId: {} 不匹配，验证失败! ", appId, appId2);
            return false;
        }

        // 验证 timestamp 是否已过期
        if (timestamp == null) {
            log.error("签名中缺少 timestamp: {}! ", appId);
            return false;
        }
        long l = Long.parseLong(timestamp);
        if (System.currentTimeMillis() - l > 5 * 60 * 1000) {
            log.error("请求已过期，appId: {}, : timestamp: {}! ", appId, timestamp);
            return false;
        }

        // 验证 nonce 是否重复请求
        if (nonce == null) {
            log.error("签名中缺少 nonce: {}! ", appId);
            return false;
        }
        String key = "cas:access:" + nonce;
        boolean lock = redisLock.lock(key, 5 * 60);
        if (!lock) {
            log.error("重复的请求 appId: {}, nonce: {} ", appId, nonce);
            return false;
        }

        return true;
    }
    */


}
