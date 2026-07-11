package com.wkclz.iam.sdk.helper;

import com.wkclz.core.exception.ValidationException;
import com.wkclz.tool.tools.Md5Tool;
import com.wkclz.tool.tools.RsaTool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 * @author shrimp
 */
@Slf4j
@Component
public class AkSignHelper {

    @Autowired(required = false)
    private RedisTemplate redisTemplate;

    /**
     * TD-003: AK 签名有效期 5 分钟
     */
    public static final long SIGN_VALIDITY_MS = 5 * 60 * 1000L;
    public static final long SIGN_VALIDITY_SECONDS = 5 * 60L;
    /**
     * TD-003: nonce 防重放 Redis Key 前缀，与签名有效期一致
     */
    public static final String NONCE_REDIS_KEY_PREFIX = "iam:ak:nonce:";

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
     * <p>
     * 与 {@link #sign(String, String)} 对应：sign 使用 RSA 私钥加密，deSign 使用 RSA 公钥解密。
     *
     * @param sign     签名字符串（请求头 sign 字段）
     * @param publicKey 服务端配置的 RSA 公钥
     * @return 解析出的参数 Map，包含 appId / nonce / timestamp
     * @throws ValidationException 解密失败或参数缺失时抛出
     */
    public static Map<String, String> deSign(String sign, String publicKey) {
        if (StringUtils.isBlank(sign)) {
            throw ValidationException.of("sign 不能为空");
        }
        if (StringUtils.isBlank(publicKey)) {
            throw ValidationException.of("publicKey 不能为空");
        }

        String decrypted;
        try {
            decrypted = RsaTool.decryptByPublicKey(sign, publicKey);
        } catch (Exception e) {
            log.warn("AK 验签失败: RSA 公钥解密异常: {}", e.getMessage());
            throw ValidationException.of("签名验签失败");
        }

        if (StringUtils.isBlank(decrypted)) {
            throw ValidationException.of("签名内容为空");
        }

        // 解析 appId=xxx&nonce=xxx&timestamp=xxx
        Map<String, String> data = new HashMap<>();
        String[] pairs = decrypted.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                data.put(pair.substring(0, idx), pair.substring(idx + 1));
            }
        }
        return data;
    }

    /**
     * <ol>
     *   <li>RSA 公钥解密签名，解析参数</li>
     *   <li>校验签名中的 appId 与请求头 app-id 一致</li>
     *   <li>校验 timestamp 在 5 分钟有效期内</li>
     *   <li>Redis SETNX 校验 nonce 唯一性，防止重放攻击</li>
     * </ol>
     *
     * @param sign          请求头中的签名
     * @param publicKey     服务端配置的 RSA 公钥
     * @param expectedAppId 请求头中的 app-id（用于与签名内容比对）
     * @return 验签通过返回 true；失败抛 ValidationException
     */
    public boolean verifySign(String sign, String publicKey, String expectedAppId) {
        Map<String, String> data = deSign(sign, publicKey);

        String signedAppId = data.get("appId");
        String nonce = data.get("nonce");
        String timestampStr = data.get("timestamp");

        if (StringUtils.isBlank(signedAppId) || StringUtils.isBlank(nonce) || StringUtils.isBlank(timestampStr)) {
            log.warn("AK 验签失败: 签名内容缺失必要参数, signedAppId={}, nonce={}, timestamp={}",
                signedAppId, nonce, timestampStr);
            throw ValidationException.of("签名内容缺失必要参数");
        }

        // 1. 校验 appId 一致
        if (!signedAppId.equals(expectedAppId)) {
            log.warn("AK 验签失败: appId 不匹配, signed={}, expected={}", signedAppId, expectedAppId);
            throw ValidationException.of("appId 不匹配");
        }

        // 2. 校验 timestamp 在 5 分钟内
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            log.warn("AK 验签失败: timestamp 格式错误, value={}", timestampStr);
            throw ValidationException.of("timestamp 格式错误");
        }
        long now = System.currentTimeMillis();
        long diff = Math.abs(now - timestamp);
        if (diff > SIGN_VALIDITY_MS) {
            log.warn("AK 验签失败: 签名已过期, timestamp={}, diff={}ms", timestamp, diff);
            throw ValidationException.of("签名已过期");
        }

        // 3. Redis SETNX 防重放（TTL 与签名有效期一致）
        if (redisTemplate != null) {
            String nonceKey = NONCE_REDIS_KEY_PREFIX + nonce;
            Boolean setOk = redisTemplate.opsForValue().setIfAbsent(nonceKey, "1", SIGN_VALIDITY_SECONDS, TimeUnit.SECONDS);
            if (setOk == null || !setOk) {
                log.warn("AK 验签失败: 检测到 nonce 重放, nonce={}", nonce);
                throw ValidationException.of("请求重放，请勿重复提交");
            }
        } else {
            log.warn("AK 验签: redisTemplate 未注入，跳过 nonce 防重放校验");
        }

        log.info("AK 验签通过, appId={}", signedAppId);
        return true;
    }

}
