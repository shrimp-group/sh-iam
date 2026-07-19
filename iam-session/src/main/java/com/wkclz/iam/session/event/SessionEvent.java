package com.wkclz.iam.session.event;

import com.wkclz.iam.session.bean.Session;
import com.wkclz.iam.session.enums.DestroyReason;

import java.io.Serializable;
import java.time.Instant;

/**
 * 会话生命周期事件 — 统一事件类，通过 {@link Type} 区分事件类型。
 *
 * <p>CREATED 时 session 非 null；DESTROYED 时 reason 非 null。</p>
 */
public class SessionEvent implements Serializable {

    public enum Type {
        CREATED, DESTROYED, EXPIRED
    }

    private final Type type;
    private final String sessionId;
    private final String subjectId;
    private final Session session;
    private final DestroyReason reason;
    private final long timestamp;

    private SessionEvent(Type type, String sessionId, String subjectId, Session session, DestroyReason reason) {
        this.type = type;
        this.sessionId = sessionId;
        this.subjectId = subjectId;
        this.session = session;
        this.reason = reason;
        this.timestamp = Instant.now().toEpochMilli();
    }

    /**
     * 创建 {@code CREATED} 事件。
     */
    public static SessionEvent created(Session session) {
        return new SessionEvent(Type.CREATED, session.getSessionId(), session.getSubjectId(), session, null);
    }

    /**
     * 创建 {@code DESTROYED} 事件。
     */
    public static SessionEvent destroyed(String sessionId, String subjectId, DestroyReason reason) {
        return new SessionEvent(Type.DESTROYED, sessionId, subjectId, null, reason);
    }

    /**
     * 创建 {@code EXPIRED} 事件。
     */
    public static SessionEvent expired(String sessionId, String subjectId) {
        return new SessionEvent(Type.EXPIRED, sessionId, subjectId, null, null);
    }

    // ========== Getters ==========

    public Type getType() {
        return type;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public Session getSession() {
        return session;
    }

    public DestroyReason getReason() {
        return reason;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "SessionEvent{type=" + type + ", sessionId='" + sessionId + "', subjectId='" + subjectId + "'" +
            (reason != null ? ", reason=" + reason : "") + "}";
    }
}
