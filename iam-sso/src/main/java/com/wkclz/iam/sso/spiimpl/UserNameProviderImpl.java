package com.wkclz.iam.sso.spiimpl;

import com.wkclz.core.spi.UserNameProvider;
import com.wkclz.iam.sso.service.UsernameCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * UserNameProvider SPI 实现
 * <p>
 * 根据 userCode 批量查询用户昵称，供 UserNameBodyAdvice 自动回填
 * BaseEntity 的 createByName / updateByName 字段。
 * <p>
 * 通过 UsernameCacheService 的 userCode 维度缓存，避免每次请求都查询数据库。
 */
@Component
public class UserNameProviderImpl implements UserNameProvider {

    @Autowired
    private UsernameCacheService usernameCacheService;

    /**
     * 根据用户编码集合批量查询用户昵称
     *
     * @param userCodes 用户编码集合（来自 BaseEntity.createBy / updateBy）
     * @return userCode -> nickname 映射
     */
    @Override
    public Map<String, String> getNamesByUserCodes(Set<String> userCodes) {
        if (userCodes == null || userCodes.isEmpty()) {
            return Map.of();
        }
        return usernameCacheService.getNicknamesByUserCodes(userCodes);
    }

}
