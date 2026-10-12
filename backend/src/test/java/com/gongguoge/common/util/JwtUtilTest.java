package com.gongguoge.common.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试。
 *
 * <p>只依赖 jjwt 与 spring-test 的 ReflectionTestUtils，不需要启动 Spring 容器、
 * 不需要 MySQL / Redis。</p>
 */
class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-key-must-be-at-least-32-bytes-long";
    private static final long EXPIRATION_SECONDS = 3600L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", EXPIRATION_SECONDS);
    }

    @Test
    @DisplayName("生成的 token 可以被校验通过")
    void generatedTokenIsValid() {
        String token = jwtUtil.generateToken(42L, "13800000000");
        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    @DisplayName("token 中携带正确的 userId 与 phone")
    void tokenCarriesUserIdAndPhone() {
        String token = jwtUtil.generateToken(42L, "13800000000");
        assertEquals(42L, jwtUtil.getUserId(token));
        Claims claims = jwtUtil.parseToken(token);
        assertEquals("13800000000", claims.get("phone", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    @DisplayName("过期 token 校验失败")
    void expiredTokenIsRejected() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -10L);
        String expired = jwtUtil.generateToken(7L, "13900000000");
        assertFalse(jwtUtil.validateToken(expired));
    }

    @Test
    @DisplayName("被篡改的 token 校验失败")
    void tamperedTokenIsRejected() {
        String token = jwtUtil.generateToken(1L, "13700000000");
        // 改动签名部分的最后一个字符
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");
        assertFalse(jwtUtil.validateToken(tampered));
    }

    @Test
    @DisplayName("用其他密钥签发的 token 校验失败")
    void tokenSignedWithOtherKeyIsRejected() {
        JwtUtil other = new JwtUtil();
        ReflectionTestUtils.setField(other, "secret", "a-completely-different-secret-key-32-bytes-min");
        ReflectionTestUtils.setField(other, "expiration", EXPIRATION_SECONDS);
        String foreignToken = other.generateToken(9L, "13600000000");
        assertFalse(jwtUtil.validateToken(foreignToken));
    }

    @Test
    @DisplayName("畸形 token 不抛异常，仅返回 false")
    void malformedTokenReturnsFalseWithoutThrowing() {
        assertFalse(jwtUtil.validateToken(""));
        assertFalse(jwtUtil.validateToken("not-a-jwt"));
        assertFalse(jwtUtil.validateToken("a.b.c"));
        assertFalse(jwtUtil.validateToken(null));
    }
}
