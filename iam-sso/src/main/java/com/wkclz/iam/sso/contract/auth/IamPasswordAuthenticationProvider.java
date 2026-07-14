package com.wkclz.iam.sso.contract.auth;

import com.wkclz.auth.bean.AuthRequest;
import com.wkclz.auth.bean.AuthResult;
import com.wkclz.auth.bean.Credential;
import com.wkclz.auth.bean.Principal;
import com.wkclz.auth.contract.auth.AuthenticationProvider;
import com.wkclz.auth.contract.auth.PasswordEncoder;
import com.wkclz.auth.enums.AuthErrorType;
import com.wkclz.auth.enums.AuthStatus;
import com.wkclz.iam.common.dto.IamUserAuthDto;
import com.wkclz.iam.sso.config.IamSsoConfig;
import com.wkclz.iam.sso.mapper.SsoLoginMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

/**
 * IAM 密码认证提供者 — SSO 专属凭证校验逻辑。
 * <p>
 * 职责：用户名查询 → 账号状态校验 → 密码匹配 → 密码过期检查。
 * 与 SSO 数据模型强耦合（IamUserAuthDto 三表 JOIN），由 sh-auth AuthenticationProvider SPI 驱动。
 * </p>
 *
 * @author shrimp
 */
@Slf4j
@Component
public class IamPasswordAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private SsoLoginMapper ssoLoginMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private IamSsoConfig iamSsoConfig;

    @Override
    public AuthResult authenticate(AuthRequest request, HttpServletRequest httpRequest) {
        Credential credential = request.getCredential();
        if (credential == null) {
            log.warn("认证凭证为空");
            return AuthResult.fail(AuthErrorType.BAD_CREDENTIALS, "凭证为空");
        }

        // 约定：credentialValue = 密码，extra.username = 用户名
        String username = (String) request.getExtra().get("username");
        String password = credential.getCredentialValue();

        log.debug("IAM 密码认证, username={}", username);

        IamUserAuthDto auth = ssoLoginMapper.getUserAuth4PasswordByUsername(username);

        // 用户不存在
        if (auth == null) {
            log.warn("用户不存在: {}", username);
            return AuthResult.fail(AuthErrorType.USERNAME_OR_PASSWORD_ERROR, "用户名或密码错误");
        }

        // 登录方式已禁用
        if (auth.getAuthStatus() != null && auth.getAuthStatus().equals(0)) {
            log.warn("登录方式已禁用: {}", username);
            return AuthResult.fail(AuthErrorType.ACCOUNT_DISABLED, "登录方式已禁用");
        }

        // 用户已锁定
        if (auth.getUserStatus() != null && auth.getUserStatus().equals(3)) {
            log.warn("用户已锁定: {}", username);
            return AuthResult.fail(AuthErrorType.ACCOUNT_LOCKED, "用户已锁定");
        }

        // 用户已禁用
        if (auth.getUserStatus() != null && auth.getUserStatus().equals(2)) {
            log.warn("用户已禁用: {}", username);
            return AuthResult.fail(AuthErrorType.ACCOUNT_DISABLED, "用户已禁用");
        }

        // 密码错误
        if (!passwordEncoder.matches(password, auth.getSalt(), auth.getPassword())) {
            log.warn("密码错误: {}", username);
            return AuthResult.fail(AuthErrorType.USERNAME_OR_PASSWORD_ERROR, "用户名或密码错误");
        }

        // 密码过期
        int passwordExpireDays = iamSsoConfig.getPasswordExpireDays();
        long passwordExpireAt = auth.getLastChangedTime()
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
            + passwordExpireDays * 24L * 60 * 60 * 1000;
        if (passwordExpireAt < System.currentTimeMillis()) {
            log.warn("密码已过期: {}", username);
            return AuthResult.fail(AuthErrorType.CREDENTIALS_EXPIRED, "密码已过期");
        }

        // 构建 Principal
        Principal principal = new Principal();
        principal.setUserCode(auth.getUserCode());
        principal.setUsername(auth.getUsername());
        principal.setNickname(auth.getNickname());
        principal.setAvatar(auth.getAvatar());
        principal.setAuthIdentifier(auth.getAuthIdentifier());
        log.info("IAM 密码认证成功, userCode={}, username={}", auth.getUserCode(), auth.getUsername());

        AuthResult result = new AuthResult();
        result.setStatus(AuthStatus.SUCCESS);
        result.setPrincipal(principal);
        return result;
    }

    @Override
    public List<String> supportedAuthTypes() {
        return Collections.singletonList("PASSWORD");
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
