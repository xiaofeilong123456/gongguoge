package com.gongguoge.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 密码存储安全性测试。
 *
 * <p>对应策划书「用户密码采用 BCrypt 加密存储，数据库中不保存明文密码」。</p>
 */
class PasswordEncoderTest {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("加密结果不包含明文密码")
    void encodedPasswordDoesNotContainPlaintext() {
        String raw = "MySecret123";
        String encoded = encoder.encode(raw);
        assertNotEquals(raw, encoded);
        assertFalse(encoded.contains(raw), "密文中不应出现明文");
    }

    @Test
    @DisplayName("BCrypt 密文格式正确（$2a$ 前缀，长度 60）")
    void encodedPasswordHasBcryptFormat() {
        String encoded = encoder.encode("anotherSecret");
        assertTrue(encoded.startsWith("$2a$"), "应为 BCrypt 前缀，实际: " + encoded.substring(0, 4));
        assertEquals(60, encoded.length(), "BCrypt 密文长度应为 60");
    }

    @Test
    @DisplayName("同一明文两次加密结果不同（含随机盐）")
    void samePlaintextProducesDifferentHashes() {
        String raw = "SamePassword1";
        String first = encoder.encode(raw);
        String second = encoder.encode(raw);
        assertNotEquals(first, second, "两次加密应因随机盐而不同");
        // 但都能校验通过
        assertTrue(encoder.matches(raw, first));
        assertTrue(encoder.matches(raw, second));
    }

    @Test
    @DisplayName("正确密码校验通过，错误密码校验失败")
    void matchesOnlyAcceptsCorrectPassword() {
        String raw = "CorrectHorse1";
        String encoded = encoder.encode(raw);
        assertTrue(encoder.matches(raw, encoded));
        assertFalse(encoder.matches("wrongPassword", encoded));
        assertFalse(encoder.matches("", encoded));
    }

    @Test
    @DisplayName("大小写敏感")
    void matchingIsCaseSensitive() {
        String encoded = encoder.encode("CaseSensitive1");
        assertFalse(encoder.matches("casesensitive1", encoded));
    }
}
