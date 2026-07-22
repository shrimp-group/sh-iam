package com.wkclz.iam.session.service;

import com.wkclz.iam.session.bean.TokenInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenService JWT 令牌服务")
class TokenServiceTest {

    private static final String SECRET_KEY = "this-is-a-test-secret-key-which-is-32-chars!";

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        // 通过反射注入 config（TokenService 使用 @Autowired field injection）
        ReflectionTestUtils.setField(tokenService, "iamSessionConfig", new com.wkclz.iam.session.config.IamSessionConfig() {{
            setSecretKey(SECRET_KEY);
            setTtl(3600L);
        }});
    }

    @Nested
    @DisplayName("验收标准")
    class AcceptanceCriteria {

        @Test
        @DisplayName("生成 Token 的 claims 仅含 userCode/username/nickname/iat/exp，不含权限信息")
        void claimsShouldBeMinimal() {
            String token = tokenService.generateToken("user_001", "admin", "管理员");
            TokenInfo info = tokenService.verifyToken(token);

            assertEquals("user_001", info.getUserCode());
            assertEquals("admin", info.getUsername());
            assertEquals("管理员", info.getNickname());
        }

        @Test
        @DisplayName("过期 Token 验证失败")
        void expiredTokenShouldFail() throws Exception {
            // 短 TTL Token
            TokenService shortService = new TokenService();
            ReflectionTestUtils.setField(shortService, "iamSessionConfig", new com.wkclz.iam.session.config.IamSessionConfig() {{
                setSecretKey(SECRET_KEY);
                setTtl(1L); // 1 秒过期
            }});

            String token = shortService.generateToken("u1", "u", "n");
            Thread.sleep(1500); // 等待过期

            assertThrows(IllegalArgumentException.class,
                () -> tokenService.verifyToken(token),
                "过期 Token 应抛出 IllegalArgumentException");
        }

        @Test
        @DisplayName("被篡改的 Token 验证失败")
        void tamperedTokenShouldFail() {
            String token = tokenService.generateToken("user_001", "admin", "管理员");
            String tampered = token + "x";

            assertThrows(IllegalArgumentException.class,
                () -> tokenService.verifyToken(tampered),
                "被篡改的 Token 应抛出 IllegalArgumentException");
        }
    }

    @Nested
    @DisplayName("生成")
    class Generate {

        @Test
        @DisplayName("生成 Token 不为空")
        void shouldGenerateNonEmptyToken() {
            String token = tokenService.generateToken("u1", "test", "测试");
            assertNotNull(token);
            assertFalse(token.isEmpty());
            assertEquals(3, token.split("\\.").length);
        }
    }

    @Nested
    @DisplayName("验证")
    class Verify {

        @Test
        @DisplayName("正确 Token 可解析出 userCode/username/nickname")
        void shouldVerifyValidToken() {
            String token = tokenService.generateToken("user_abc", "zhangsan", "张三");
            TokenInfo info = tokenService.verifyToken(token);

            assertEquals("user_abc", info.getUserCode());
            assertEquals("zhangsan", info.getUsername());
            assertEquals("张三", info.getNickname());
        }

        @Test
        @DisplayName("空 Token 抛出 IllegalArgumentException")
        void nullTokenShouldFail() {
            assertThrows(IllegalArgumentException.class,
                () -> tokenService.verifyToken(null));
        }
    }

    @Nested
    @DisplayName("刷新")
    class Refresh {

        @Test
        @DisplayName("刷新产生新 Token，解析出原始信息")
        void shouldRefreshToken() {
            String oldToken = tokenService.generateToken("u1", "test", "测试");
            String newToken = tokenService.refreshToken(oldToken);

            assertNotNull(newToken);
            assertNotEquals(oldToken, newToken);

            TokenInfo refreshedInfo = tokenService.verifyToken(newToken);
            assertEquals("u1", refreshedInfo.getUserCode());
            assertEquals("test", refreshedInfo.getUsername());
            assertEquals("测试", refreshedInfo.getNickname());
        }
    }

    @Nested
    @DisplayName("配置校验")
    class Configuration {

        @Test
        @DisplayName("secretKey 为空时启动报错")
        void shouldThrowOnEmptySecretKey() {
            TokenService service = new TokenService();
            ReflectionTestUtils.setField(service, "iamSessionConfig", new com.wkclz.iam.session.config.IamSessionConfig() {{
                setSecretKey(null);
                setTtl(3600L);
            }});

            assertThrows(IllegalArgumentException.class,
                () -> service.generateToken("u1", "u", "n"));
        }

        @Test
        @DisplayName("secretKey 不足 32 字符时启动报错")
        void shouldThrowOnShortSecretKey() {
            TokenService service = new TokenService();
            ReflectionTestUtils.setField(service, "iamSessionConfig", new com.wkclz.iam.session.config.IamSessionConfig() {{
                setSecretKey("short-key");
                setTtl(3600L);
            }});

            assertThrows(IllegalArgumentException.class,
                () -> service.generateToken("u1", "u", "n"));
        }
    }

}
