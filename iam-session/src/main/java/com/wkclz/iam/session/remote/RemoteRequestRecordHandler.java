package com.wkclz.iam.session.remote;

import com.wkclz.iam.session.bean.RequestRecord;
import com.wkclz.iam.session.spi.RequestRecordHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 远程请求日志持久化处理器 — 第三方应用部署时通过远程 SsoFacade 持久化日志。
 *
 * <p>实现 {@link RequestRecordHandler} SPI，内部委托 {@link SsoFacade#saveLog} 远程 HTTP 调用 SSO 服务端。</p>
 *
 * <p>条件装配：仅当 {@code iam.session.remote.server-url} 配置时注册。
 * 与 {@link RemoteSsoFacadeImpl} 联动，替代已删除的 LoggingFilter 的远程持久化职责。</p>
 *
 * <p>注册优先级：本类通过 {@code @ConditionalOnProperty} 注册后，
 * {@code NoOpRequestRecordHandler} 的 {@code @ConditionalOnMissingBean} 条件不满足，不会注册。
 * 服务端部署时本类不注册，iam-sso 的 {@code RequestRecordHandlerImpl} 注册。</p>
 */
@Component
@ConditionalOnProperty(name = "iam.session.remote.server-url")
public class RemoteRequestRecordHandler implements RequestRecordHandler {

    private static final Logger log = LoggerFactory.getLogger(RemoteRequestRecordHandler.class);

    @Autowired
    private SsoFacade ssoFacade;

    @Override
    public void handle(RequestRecord record) {
        try {
            ssoFacade.saveLog(record);
        } catch (Exception e) {
            log.warn("Failed to persist request log remotely: uri={}, error={}",
                record.getRequestUri(), e.getMessage());
        }
    }
}
