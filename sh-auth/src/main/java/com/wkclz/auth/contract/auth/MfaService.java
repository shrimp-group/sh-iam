package com.wkclz.auth.contract.auth;

import com.wkclz.auth.bean.MfaChallenge;
import com.wkclz.auth.enums.MfaType;

public interface MfaService {
    boolean isMfaRequired(String subjectId);
    MfaChallenge sendChallenge(String subjectId, MfaType type);
    boolean verifyChallenge(String challengeId, String code);
}
