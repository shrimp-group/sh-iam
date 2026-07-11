package com.wkclz.auth.cache;

import com.wkclz.auth.enums.RefreshScope;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 缓存刷新事件（IAM admin 配置变更时发布）
 */
@Getter
public class AuthCacheRefreshEvent extends ApplicationEvent {

    private final String appCode;
    private final String subjectId;
    private final RefreshScope scope;

    public AuthCacheRefreshEvent(Object source, String appCode, String subjectId, RefreshScope scope) {
        super(source);
        this.appCode = appCode;
        this.subjectId = subjectId;
        this.scope = scope;
    }
}
