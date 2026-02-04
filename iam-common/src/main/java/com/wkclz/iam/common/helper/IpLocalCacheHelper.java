package com.wkclz.iam.common.helper;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSONPath;
import com.wkclz.iam.common.entity.IamRequestLog;
import com.wkclz.web.helper.IpHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author shrimp
 */
@Slf4j
public class IpLocalCacheHelper {

    private static final String IP_LOCATION_SERVER = "http://opendata.baidu.com/api.php?resource_id=6006&query=";


    private static final Queue<String> IP_QUEUE = new ConcurrentLinkedQueue<>();
    private static final Map<String, IamRequestLog> IP_ADDR_CACHE = new ConcurrentHashMap<>();

    // 写队列
    public static synchronized IamRequestLog offerQueue(String remoteAddr) {
        if (StringUtils.isBlank(remoteAddr)) {
            return null;
        }

        // 不为空，实时返回
        IamRequestLog authRequestLog = IP_ADDR_CACHE.get(remoteAddr);
        if (authRequestLog != null) {
            return authRequestLog;
        }

        // 队列已存在，就不需要再加了
        if (IP_QUEUE.contains(remoteAddr)) {
            return null;
        }

        // 为空，写队列
        boolean added = IP_QUEUE.offer(remoteAddr);
        if (added) {
            log.info("缓存未出现此IP, 已添加到队列: " + remoteAddr);
        }
        return null;
    }


    public static String pollQueue() {
        String remoteAddr = IP_QUEUE.poll();
        if (remoteAddr == null) {
            return null;
        }
        return remoteAddr;
    }


    // 从网络上解析 IP 为地址
    public static synchronized IamRequestLog getLocation(String remoteAddr) {
        if (StringUtils.isBlank(remoteAddr)) {
            return null;
        }
        IamRequestLog authRequestLog = IP_ADDR_CACHE.get(remoteAddr);
        if (authRequestLog != null) {
            return authRequestLog;
        }
        IamRequestLog requestLog = new IamRequestLog();
        requestLog.setRemoteAddr(remoteAddr);

        // 局域网 IP
        if (IpHelper.isInnerAddress (remoteAddr)) {
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
