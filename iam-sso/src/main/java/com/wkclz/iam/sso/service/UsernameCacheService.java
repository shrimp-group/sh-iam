package com.wkclz.iam.sso.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.wkclz.iam.common.entity.IamUser;
import com.wkclz.iam.sso.mapper.SsoLoginMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 用户编码 -> 用户姓名 缓存服务
 * <p>
 * 基于 Guava LoadingCache 实现，的缓存：
 * <ul>
 *   <li>userCodeCache: userCode -> nickname，供 UserNameProvider SPI 使用</li>
 * </ul>
 * 缓存的淘汰策略：
 * <ul>
 *   <li>基于访问频率的淘汰策略：频繁访问的条目保留，冷数据自动淘汰</li>
 *   <li>容量上限控制：最大缓存条目数受限，防止内存溢出</li>
 *   <li>定时过期：写入后最大存活时间 + 访问后空闲过期，双重保障数据新鲜度</li>
 *   <li>防缓存穿透：DB 中不存在的键也会缓存（Optional.empty），避免重复击穿</li>
 *   <li>批量加载：内部实现 loadAll，多次单键查询自动合并为一次批量 DB 查询</li>
 * </ul>
 */
@Slf4j
@Service
public class UsernameCacheService {

    /**
     * 缓存最大条目数，控制内存占用量
     */
    private static final long MAXIMUM_SIZE = 10000;

    /**
     * 写入后过期时间（分钟）：无论是否访问，超过此时间强制过期，保证数据最终一致性
     */
    private static final long EXPIRE_AFTER_WRITE_MINUTES = 60;

    /**
     * 访问后过期时间（分钟）：长时间不访问的条目自动清除，释放内存
     */
    private static final long EXPIRE_AFTER_ACCESS_MINUTES = 30;

    /**
     * Guava LoadingCache：key=用户编码, value=Optional<昵称>
     * <p>
     * 供 UserNameProvider SPI 使用，根据 userCode 批量查询昵称，
     * 用于自动回填 BaseEntity 的 createByName / updateByName 字段。
     */
    private final LoadingCache<String, Optional<String>> userCodeCache;

    @Autowired
    private SsoLoginMapper ssoLoginMapper;

    public UsernameCacheService() {

        userCodeCache = buildCache(new CacheLoader<String, Optional<String>>() {

            /**
             * 单键加载：查询单个用户编码对应的昵称
             */
            @Override
            public Optional<String> load(String userCode) {
                List<IamUser> users = ssoLoginMapper.batchGetNicknamesByUserCodes(Collections.singletonList(userCode));
                if (users.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.ofNullable(users.getFirst().getNickname());
            }

            /**
             * 批量加载：一次性查询多个用户编码对应的昵称
             */
            public Map<? extends String, Optional<String>> loadAll(Set<? extends String> userCodes) {
                List<IamUser> users = ssoLoginMapper.batchGetNicknamesByUserCodes(new ArrayList<>(userCodes));

                Map<String, Optional<String>> result = new LinkedHashMap<>();
                for (String userCode : userCodes) {
                    result.put(userCode, Optional.empty());
                }

                for (IamUser user : users) {
                    result.put(user.getUserCode(), Optional.ofNullable(user.getNickname()));
                }

                return result;
            }
        });
    }

    /**
     * 批量查询：根据用户编码集合获取 userCode -> nickname 映射
     * <p>
     * 供 UserNameProvider SPI 使用，自动回填 BaseEntity 的 createByName / updateByName。
     *
     * @param userCodes 用户编码集合
     * @return userCode -> nickname 映射
     */
    public Map<String, String> getNicknamesByUserCodes(Collection<String> userCodes) {
        if (userCodes == null || userCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> validUserCodes = new LinkedHashSet<>();
        for (String userCode : userCodes) {
            if (userCode != null && !userCode.isEmpty()) {
                validUserCodes.add(userCode);
            }
        }
        if (validUserCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new LinkedHashMap<>();
        try {
            Map<String, Optional<String>> cached = userCodeCache.getAll(validUserCodes);
            cached.forEach((userCode, nicknameOpt) -> nicknameOpt.ifPresent(nickname -> result.put(userCode, nickname)));
        } catch (Exception e) {
            log.warn("can not load nickname by userCode: {}: {}", userCodes, e.getMessage());
        }
        return result;
    }

    /**
     * 单个查询：根据用户编码获取昵称
     *
     * @param userCode 用户编码
     * @return 昵称，用户不存在或昵称为空时返回 null
     */
    public String getNicknameByUserCode(String userCode) {
        if (userCode == null || userCode.isEmpty()) {
            return null;
        }
        try {
            Optional<String> nicknameOpt = userCodeCache.get(userCode);
            return nicknameOpt.orElse(null);
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * 驱逐 userCode 维度的单个缓存条目
     *
     * @param userCode 需要失效的用户编码
     */
    public void evict(String userCode) {
        if (userCode != null) {
            userCodeCache.invalidate(userCode);
        }
    }

    /**
     * 清空全部缓存
     * <p>
     * 在批量用户数据变更等场景下使用
     */
    public void evictAll() {
        userCodeCache.invalidateAll();
    }

    /**
     * 获取 userCode 缓存统计信息
     */
    public CacheStats getUserCodeCacheStats() {
        return userCodeCache.stats();
    }

    /**
     * 构建 Guava LoadingCache 实例，统一缓存策略参数
     */
    private LoadingCache<String, Optional<String>> buildCache(CacheLoader<String, Optional<String>> loader) {
        return CacheBuilder.newBuilder()
                .maximumSize(MAXIMUM_SIZE)
                .expireAfterWrite(EXPIRE_AFTER_WRITE_MINUTES, TimeUnit.MINUTES)
                .expireAfterAccess(EXPIRE_AFTER_ACCESS_MINUTES, TimeUnit.MINUTES)
                .recordStats()
                .build(loader);
    }

}
