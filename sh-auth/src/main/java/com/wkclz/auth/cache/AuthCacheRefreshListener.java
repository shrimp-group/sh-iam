package com.wkclz.auth.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 缓存刷新事件监听器（仅当 AuthCacheManager Bean 存在时启用）
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(AuthCacheManager.class)
public class AuthCacheRefreshListener {

    private final AuthCacheManager cacheManager;

    @EventListener
    public void onRefresh(AuthCacheRefreshEvent event) {
        log.info("收到缓存刷新事件: scope={}, appCode={}, subjectId={}",
                event.getScope().getDesc(), event.getAppCode(), event.getSubjectId());

        switch (event.getScope()) {
            case METADATA -> {
                cacheManager.refreshMetadata(event.getAppCode());
                log.info("已刷新应用元数据缓存并级联清空权限计算结果: appCode={}", event.getAppCode());
            }
            case SUBJECT -> {
                cacheManager.evictSubject(event.getSubjectId(), event.getAppCode());
                log.info("已清理用户授权缓存: subjectId={}, appCode={}", event.getSubjectId(), event.getAppCode());
            }
            case ALL -> {
                cacheManager.refreshMetadata(event.getAppCode());
                if (event.getSubjectId() != null) {
                    cacheManager.evictSubject(event.getSubjectId(), event.getAppCode());
                }
                log.info("已刷新全部缓存");
            }
        }
    }
}
