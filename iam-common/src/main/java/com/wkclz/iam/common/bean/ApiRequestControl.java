package com.wkclz.iam.common.bean;

import com.alibaba.fastjson2.JSON;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * API 请求控制配置模型（纯 Bean，不包含任何 JSON 解析逻辑）
 * enable 默认 false，需显式开启；与全局开关 iam.request-control.enabled（默认 true）含义不同
 *
 * @author shrimp
 */
@Data
public class ApiRequestControl {

    /**
     * 请求控制总开关
     */
    @Schema(description = "请求控制总开关：true-开启，false/空-关闭")
    private Boolean enable;

    /**
     * 互斥控制配置
     */
    @Schema(description = "互斥控制配置")
    private Mutex mutex;

    /**
     * 限流控制配置
     */
    @Schema(description = "限流控制配置")
    private RateLimit rateLimit;

    /**
     * 互斥控制配置项
     *
     * @author shrimp
     */
    @Data
    public static class Mutex {

        /**
         * 互斥开关
         */
        @Schema(description = "互斥开关")
        private Boolean enable;

        /**
         * 互斥锁超时秒，默认 30
         */
        @Schema(description = "互斥锁超时秒，默认 30")
        private Integer timeoutSeconds;
    }

    /**
     * 限流控制配置项
     *
     * @author shrimp
     */
    @Data
    public static class RateLimit {

        /**
         * 限流开关
         */
        @Schema(description = "限流开关")
        private Boolean enable;

        /**
         * 窗口秒，默认 60
         */
        @Schema(description = "窗口秒，默认 60")
        private Integer windowSeconds;

        /**
         * 窗口内最大请求次数，默认 100
         */
        @Schema(description = "窗口内最大请求次数，默认 100")
        private Integer maxRequests;
    }

    /**
     * 从数据库 JSON 字符串解析配置；空白或解析失败返回 null
     *
     * @param json JSON 字符串
     * @return 请求控制配置对象；空白或解析失败返回 null
     */
    public static ApiRequestControl parse(String json) {
        if (StringUtils.isBlank(json)) {
            return null;
        }
        try {
            return JSON.parseObject(json, ApiRequestControl.class);
        } catch (Exception e) {
            return null;
        }
    }
}
