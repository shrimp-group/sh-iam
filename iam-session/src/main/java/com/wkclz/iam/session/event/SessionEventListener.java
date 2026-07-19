package com.wkclz.iam.session.event;

import com.wkclz.iam.session.bean.Session;
import com.wkclz.iam.session.enums.DestroyReason;

/**
 * 会话生命周期事件监听器 SPI。
 *
 * <p>上层模块实现此接口，通过 Spring 的 {@code @EventListener} 或
 * {@code ApplicationListener} 消费会话事件，实现审计日志等扩展。</p>
 */
public interface SessionEventListener {

    /**
     * 会话创建成功。
     */
    void onCreated(Session session);

    /**
     * 会话被销毁（主动登出、改密、禁用等）。
     */
    void onDestroyed(String sessionId, String subjectId, DestroyReason reason);

    /**
     * 会话自然过期。
     */
    void onExpired(String sessionId, String subjectId);

}
