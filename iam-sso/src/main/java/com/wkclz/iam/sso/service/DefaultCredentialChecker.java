package com.wkclz.iam.sso.service;

import com.wkclz.core.identity.UserIdentity;
import com.wkclz.iam.common.dto.IamUserAuthDto;
import com.wkclz.iam.sso.config.IamSsoConfig;
import com.wkclz.iam.sso.mapper.SsoLoginMapper;
import com.wkclz.iam.sso.spi.CredentialChecker;
import com.wkclz.iam.sso.spi.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 默认凭证校验实现 — 查询数据库，校验状态 → 密码 → 过期。
 *
 * <p>校验顺序严格：存在 → 禁用 → 锁定 → 认证禁用 → 密码匹配 → 密码过期。
 * 密码匹配成功后，若为旧格式（{MD5}/无前缀）则自动升级存储为 {PBKDF2}。</p>
 */
@Component
public class DefaultCredentialChecker implements CredentialChecker {

    private static final Logger log = LoggerFactory.getLogger(DefaultCredentialChecker.class);

    @Autowired
    private SsoLoginMapper ssoLoginMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private IamSsoConfig iamSsoConfig;

    @Override
    public CredentialCheckResult check(String username, String rawPassword) {
        // 1. 查用户信息
        IamUserAuthDto dto = ssoLoginMapper.getUserAuth4PasswordByUsername(username);
        if (dto == null) {
            log.warn("User not found: username={}", username);
            return CredentialCheckResult.fail(CredentialCheckResult.FailReason.USER_NOT_FOUND);
        }

        // 2. userStatus 检查
        Integer userStatus = dto.getUserStatus();
        if (userStatus != null && userStatus == 2) {
            log.warn("User disabled: username={}", username);
            return CredentialCheckResult.fail(CredentialCheckResult.FailReason.DISABLED);
        }
        if (userStatus != null && userStatus == 3) {
            log.warn("User locked: username={}", username);
            return CredentialCheckResult.fail(CredentialCheckResult.FailReason.LOCKED);
        }

        // 3. authStatus 检查（认证方式是否启用）
        Integer authStatus = dto.getAuthStatus();
        if (authStatus != null && authStatus == 0) {
            log.warn("Auth disabled: username={}", username);
            return CredentialCheckResult.fail(CredentialCheckResult.FailReason.AUTH_DISABLED);
        }

        // 4. 密码匹配
        String storedPassword = dto.getPassword();
        String salt = dto.getSalt();
        if (!passwordEncoder.matches(rawPassword, salt, storedPassword)) {
            log.warn("Password mismatch: username={}", username);
            return CredentialCheckResult.fail(CredentialCheckResult.FailReason.PASSWORD_ERROR);
        }

        // 5. 旧格式密码自动升级
        if (passwordEncoder instanceof Pbkdf2PasswordEncoder pbkdf2) {
            if (pbkdf2.needsUpgrade(storedPassword)) {
                String newPassword = passwordEncoder.encode(rawPassword, salt);
                // 直接更新密码存储（salt 不变，仅升级 hash 格式）
                ssoLoginMapper.updatePasswordByUserCode(buildPasswordEntity(dto.getUserCode(), newPassword, salt));
                log.info("Password upgraded to PBKDF2 for user: {}", username);
            }
        }

        // 6. 构建 UserIdentity
        UserIdentity userIdentity = new UserIdentity();
        userIdentity.setUserCode(dto.getUserCode());
        userIdentity.setUsername(dto.getUsername());
        userIdentity.setNickname(dto.getNickname());
        userIdentity.setAvatar(dto.getAvatar());

        // 7. 密码过期检查
        LocalDateTime lastChangedTime = dto.getLastChangedTime();
        Integer expireDays = iamSsoConfig.getPasswordExpireDays();
        if (lastChangedTime != null && expireDays != null && expireDays > 0) {
            if (lastChangedTime.plusDays(expireDays).isBefore(LocalDateTime.now())) {
                log.warn("Password expired: username={}, lastChanged={}", username, lastChangedTime);
                return CredentialCheckResult.fail(CredentialCheckResult.FailReason.PASSWORD_EXPIRED, userIdentity);
            }
        }

        return CredentialCheckResult.success(userIdentity);
    }

    private com.wkclz.iam.common.entity.IamUserAuthPassword buildPasswordEntity(String userCode, String password, String salt) {
        com.wkclz.iam.common.entity.IamUserAuthPassword entity = new com.wkclz.iam.common.entity.IamUserAuthPassword();
        entity.setUserCode(userCode);
        entity.setPassword(password);
        entity.setSalt(salt);
        entity.setLastChangedTime(LocalDateTime.now());
        return entity;
    }
}
