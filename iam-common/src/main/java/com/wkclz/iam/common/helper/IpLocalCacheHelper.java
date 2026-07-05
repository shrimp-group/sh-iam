package com.wkclz.iam.common.helper;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONPath;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wkclz.iam.common.entity.IamRequestLog;
import com.wkclz.tool.utils.NetworkUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author shrimp
 */
@Slf4j
public class IpLocalCacheHelper {

    private static final String IP_LOCATION_SERVER = "https://opendata.baidu.com/api.php?resource_id=6006&query=";

    private static final Set<String> IP_QUEUED_SET = ConcurrentHashMap.newKeySet();
    private static final Queue<String> IP_QUEUE = new ConcurrentLinkedQueue<>();

    private static final Cache<String, IamRequestLog> IP_ADDR_CACHE = CacheBuilder.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build();

    // 写队列
    public static IamRequestLog offerQueue(String remoteAddr) {
        if (StringUtils.isBlank(remoteAddr)) {
            return null;
        }

        // 不为空，实时返回
        IamRequestLog authRequestLog = IP_ADDR_CACHE.getIfPresent(remoteAddr);
        if (authRequestLog != null) {
            return authRequestLog;
        }

        // Set.add 是原子操作：返回 true 表示之前不存在（成功入队），返回 false 表示已存在
        boolean added = IP_QUEUED_SET.add(remoteAddr);
        if (added) {
            IP_QUEUE.offer(remoteAddr);
            log.info("缓存未出现此IP, 已添加到队列: " + remoteAddr);
        }
        return null;
    }


    public static String pollQueue() {
        String remoteAddr = IP_QUEUE.poll();
        if (remoteAddr == null) {
            return null;
        }
        // 出队后从去重 Set 中移除，允许后续再次入队
        IP_QUEUED_SET.remove(remoteAddr);
        return remoteAddr;
    }


    // 从网络上解析 IP 为地址
    public static IamRequestLog getLocation(String remoteAddr) {
        if (StringUtils.isBlank(remoteAddr)) {
            return null;
        }
        IamRequestLog authRequestLog = IP_ADDR_CACHE.getIfPresent(remoteAddr);
        if (authRequestLog != null) {
            return authRequestLog;
        }
        IamRequestLog requestLog = new IamRequestLog();
        requestLog.setRemoteAddr(remoteAddr);

        // 局域网 IP
        if (NetworkUtil.isInnerAddress(remoteAddr)) {
            requestLog.setLocation("本地局域网");
            requestLog.setIsp("本地局域网");
            IP_ADDR_CACHE.put(remoteAddr, requestLog);
            return requestLog;
        }

        String url = IP_LOCATION_SERVER + remoteAddr + "&_=" + System.currentTimeMillis();

        log.info("remoteAddr 2 location ---> url : {} ", url);

        HttpRequest get = HttpUtil.createGet(url);
        String body = get.execute().body();
        Object status = JSONPath.extract(body, "$.status");
        Object locationObj = JSONPath.extract(body, "$.data[0].location");


        if (status != null && "0".equals(status.toString()) && locationObj != null) {
            String location = locationObj.toString();

            int spr = location.indexOf(" ");
            if (spr != -1) {
                requestLog.setLocation(location.substring(0, spr));
                requestLog.setIsp(location.substring(spr+1));
            } else {
                requestLog.setLocation(location);
                requestLog.setIsp(location);
            }
        } else {
            requestLog.setLocation("未知");
            requestLog.setIsp("未知");
        }
        IP_ADDR_CACHE.put(remoteAddr, requestLog);
        return requestLog;
    }

}
