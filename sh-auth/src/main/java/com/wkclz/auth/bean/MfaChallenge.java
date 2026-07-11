package com.wkclz.auth.bean;

import com.wkclz.auth.enums.MfaType;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/** MFA 挑战 */
@Data
public class MfaChallenge implements Serializable {
    private String challengeId;
    private MfaType mfaType;
    private String target;
    private LocalDateTime expireTime;
    private boolean used;
}
