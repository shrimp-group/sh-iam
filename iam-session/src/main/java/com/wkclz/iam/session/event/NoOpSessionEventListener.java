package com.wkclz.iam.session.event;

import com.wkclz.iam.session.bean.Session;
import com.wkclz.iam.session.enums.DestroyReason;

/**
 * {@link SessionEventListener} 的默认空实现。
 *
 * <p>上层模块未注入自定义实现时，Spring 自动装配此 Bean，避免 NPE。</p>
 */
public class NoOpSessionEventListener implements SessionEventListener {

    @Override
    public void onCreated(Session session) {
        // no-op
    }

    @Override
    public void onDestroyed(String sessionId, String subjectId, DestroyReason reason) {
        // no-op
    }

    @Override
    public void onExpired(String sessionId, String subjectId) {
        // no-op
    }

}
