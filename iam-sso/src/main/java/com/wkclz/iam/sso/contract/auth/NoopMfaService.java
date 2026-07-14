package com.wkclz.iam.sso.contract.auth;

import com.wkclz.auth.bean.MfaChallenge;
import com.wkclz.auth.contract.auth.MfaService;
import com.wkclz.auth.enums.MfaType;
import org.springframework.stereotype.Component;

/**
 * MFA 服务空实现 — IAM 暂不支持 MFA 多因素认证。
 *
 * @author shrimp
 */
@Component
public class NoopMfaService implements MfaService {

    @Override
    public boolean isMfaRequired(String subjectId) {
        return false;
    }

    @Override
    public MfaChallenge sendChallenge(String subjectId, MfaType type) {
        return null;
    }

    @Override
    public boolean verifyChallenge(String challengeId, String code) {
        return false;
    }
}
