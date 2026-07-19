package com.wkclz.iam.session.event;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

/**
 * 会话销毁事件 — 在会话被销毁后发布，供审计等监听器消费。
 *
 * <p>Spring 4.2+ 支持任意对象作为事件，无需继承 ApplicationEvent。</p>
 */
@Data
public class SessionDestroyedEvent implements Serializable {

    /**
     * 被销毁的会话 ID
     */
    private String sessionId;

    /**
     * 会话所属用户标识
     */
    private String subjectId;

    /**
     * 销毁原因（如 logout、admin_action、password_changed 等）
     */
    private String reason;

    /**
     * 事件发生时间
     */
    private long timestamp;


    public SessionDestroyedEvent() {
    }

    public SessionDestroyedEvent(String sessionId, String subjectId, String reason) {
        this.sessionId = sessionId;
        this.subjectId = subjectId;
        this.reason = reason;
        this.timestamp = Instant.now().toEpochMilli();
    }

}
