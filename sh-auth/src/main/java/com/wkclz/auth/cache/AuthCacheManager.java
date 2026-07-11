package com.wkclz.auth.cache;

import com.wkclz.auth.bean.AuthMetadata;
import com.wkclz.auth.bean.ResolvedAuthorization;
import com.wkclz.auth.bean.SubjectAuthorization;
import com.wkclz.auth.contract.infra.AuthMetadataService;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.time.Duration;

/**
 * 三层缓存管理器
 */
public class AuthCacheManager {

    private final AuthMetadataService metadataService;

    /** L1: 应用级元数据缓存（单例，所有用户共享） */
    private final LoadingCache<String, AuthMetadata> metadataCache;

    /** L2: 用户级授权数据缓存（每用户，仅角色码 + 数据权限） */
    private final LoadingCache<String, SubjectAuthorization> subjectAuthCache;

    /** L3: 权限计算结果缓存（每用户，O(1) 查找） */
    private final LoadingCache<String, ResolvedAuthorization> resolvedAuthCache;

    public AuthCacheManager(AuthMetadataService metadataService) {
        this.metadataService = metadataService;

        this.metadataCache = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofHours(1))
                .build(new CacheLoader<>() {
                    @Override
                    public AuthMetadata load(String appCode) {
                        return metadataService.loadMetadata(appCode);
                    }
                });

        this.subjectAuthCache = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30))
                .build(new CacheLoader<>() {
                    @Override
                    public SubjectAuthorization load(String key) {
                        String[] parts = key.split(":", 2);
                        return metadataService.loadSubjectAuth(parts[0], parts[1]);
                    }
                });

        this.resolvedAuthCache = CacheBuilder.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(30))
                .build(new CacheLoader<>() {
                    @Override
                    public ResolvedAuthorization load(String key) {
                        // 由外部调用方负责计算并设置
                        return null;
                    }
                });
    }

    public AuthMetadata getMetadata(String appCode) {
        return metadataCache.getUnchecked(appCode);
    }

    public SubjectAuthorization getSubjectAuth(String subjectId, String appCode) {
        return subjectAuthCache.getUnchecked(cacheKey(subjectId, appCode));
    }

    public ResolvedAuthorization getResolvedAuth(String subjectId, String appCode) {
        return resolvedAuthCache.getIfPresent(cacheKey(subjectId, appCode));
    }

    public void putResolvedAuth(String subjectId, String appCode, ResolvedAuthorization resolved) {
        resolvedAuthCache.put(cacheKey(subjectId, appCode), resolved);
    }

    /** 逐出指定用户的缓存 */
    public void evictSubject(String subjectId, String appCode) {
        String key = cacheKey(subjectId, appCode);
        subjectAuthCache.invalidate(key);
        resolvedAuthCache.invalidate(key);
    }

    /** 刷新应用元数据 → 级联清空所有用户的权限计算结果 */
    public void refreshMetadata(String appCode) {
        metadataCache.refresh(appCode);
        resolvedAuthCache.invalidateAll();
    }

    private String cacheKey(String subjectId, String appCode) {
        return subjectId + ":" + appCode;
    }
}
