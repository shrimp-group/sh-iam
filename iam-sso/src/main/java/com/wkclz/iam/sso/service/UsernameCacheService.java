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
 * 用户名 -> 用户姓名 缓存服务
 * <p>
 * 基于 Guava LoadingCache 实现，具备以下特性：
 * <ul>
 *   <li>基于访问频率的淘汰策略：频繁访问的条目保留，冷数据自动淘汰</li>
 *   <li>容量上限控制：最大缓存条目数受限，防止内存溢出</li>
 *   <li>定时过期：写入后最大存活时间 + 访问后空闲过期，双重保障数据新鲜度</li>
 *   <li>防缓存穿透：DB 中不存在的用户名也会缓存（Optional.empty），避免重复击穿</li>
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
     * Guava LoadingCache：key=用户名, value=Optional<昵称>
     * <p>
     * 使用 Optional 包装昵称，以区分"缓存了但DB中不存在"与"未缓存"两种状态，
     * 防止对不存在的用户名反复穿透到数据库。
     */
    private final LoadingCache<String, Optional<String>> cache;

    @Autowired
    private SsoLoginMapper ssoLoginMapper;

    public UsernameCacheService() {
        cache = CacheBuilder.newBuilder()
                .maximumSize(MAXIMUM_SIZE)
                .expireAfterWrite(EXPIRE_AFTER_WRITE_MINUTES, TimeUnit.MINUTES)
                .expireAfterAccess(EXPIRE_AFTER_ACCESS_MINUTES, TimeUnit.MINUTES)
                .recordStats()
                .build(new CacheLoader<String, Optional<String>>() {

                    /**
                     * 单键加载：查询单个用户名对应的昵称
                     * 当 cache.get(key) 未命中时触发
                     */
                    @Override
                    public Optional<String> load(String username) {
                        List<IamUser> users = ssoLoginMapper.batchGetNicknamesByUsernames(Collections.singletonList(username));
                        if (users.isEmpty()) {
                            return Optional.empty();
                        }
                        return Optional.ofNullable(users.getFirst().getNickname());
                    }

                    /**
                     * 批量加载：一次性查询多个用户名对应的昵称
                     * 当 cache.getAll(keys) 中有未命中键时触发，将未命中的键合并为一次 DB 查询
                     */
                    public Map<? extends String, Optional<String>> loadAll(Set<? extends String> usernames) {
                        List<IamUser> users = ssoLoginMapper.batchGetNicknamesByUsernames(new ArrayList<>(usernames));

                        // 先将所有待查键标记为"未找到"
                        Map<String, Optional<String>> result = new LinkedHashMap<>();
                        for (String username : usernames) {
                            result.put(username, Optional.empty());
                        }

                        // 用 DB 实际结果覆盖"未找到"标记
                        for (IamUser user : users) {
                            result.put(user.getUsername(), Optional.ofNullable(user.getNickname()));
                        }

                        return result;
                    }
                });
    }

    /**
     * 批量查询：根据用户名集合获取 username -> nickname 映射
     * <p>
     * 已缓存的键直接返回，未缓存的键通过 loadAll 批量从 DB 加载并回填缓存。
     * DB 中不存在的用户名不会出现在返回结果中。
     *
     * @param usernames 用户名集合
     * @return username -> nickname 映射，仅包含 DB 中实际存在且有昵称的用户
     */
    public Map<String, String> getNicknamesByUsernames(Collection<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return Collections.emptyMap();
        }

        // 过滤无效输入，同时去重
        Set<String> validUsernames = new LinkedHashSet<>();
        for (String username : usernames) {
            if (username != null && !username.isEmpty()) {
                validUsernames.add(username);
            }
        }
        if (validUsernames.isEmpty()) {
            return Collections.emptyMap();
        }

        // 从缓存获取（未命中的键会触发 loadAll 批量加载）
        Map<String, String> result = new LinkedHashMap<>();
        try {
            Map<String, Optional<String>> cached = cache.getAll(validUsernames);

            // 组装结果：仅包含有昵称的条目
            cached.forEach((username, nicknameOpt) -> nicknameOpt.ifPresent(nickname -> result.put(username, nickname)));
        } catch (Exception e) {
            log.warn("can not load nickname by username: {}: {}", usernames, e.getMessage());
        }
        return result;
    }

    /**
     * 单个查询：根据用户名获取昵称
     *
     * @param username 用户名
     * @return 昵称，用户不存在或昵称为空时返回 null
     */
    public String getNicknameByUsername(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        try {
            Optional<String> nicknameOpt = cache.get(username);
            return nicknameOpt.orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 驱逐单个缓存条目
     * <p>
     * 在用户昵称变更时调用，确保下次查询获取最新数据
     *
     * @param username 需要失效的用户名
     */
    public void evict(String username) {
        if (username != null) {
            cache.invalidate(username);
        }
    }

    /**
     * 清空全部缓存
     * <p>
     * 在批量用户数据变更等场景下使用
     */
    public void evictAll() {
        cache.invalidateAll();
    }

    /**
     * 获取缓存统计信息，用于监控和调优
     *
     * @return Guava CacheStats 包含命中率、加载次数、淘汰次数等指标
     */
    public CacheStats getCacheStats() {
        return cache.stats();
    }

}
