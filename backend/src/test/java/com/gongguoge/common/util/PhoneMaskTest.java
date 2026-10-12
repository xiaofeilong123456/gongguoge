package com.gongguoge.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 手机号脱敏测试。
 *
 * <p>手机号属个人信息，日志中必须脱敏，这里锁定行为避免以后被改回去。</p>
 */
class PhoneMaskTest {

    @Test
    @DisplayName("标准 11 位手机号保留前 3 后 4")
    void masksStandardPhone() {
        assertEquals("138****1234", PhoneMask.mask("13800001234"));
    }

    @Test
    @DisplayName("带前后空格的手机号也能正确脱敏")
    void masksTrimmedPhone() {
        assertEquals("138****1234", PhoneMask.mask("  13800001234  "));
    }

    @Test
    @DisplayName("过短的字符串整体隐去，不泄露任何字符")
    void masksShortInput() {
        assertEquals("***", PhoneMask.mask("12345"));
        assertEquals("***", PhoneMask.mask(""));
    }

    @Test
    @DisplayName("null 返回 null，不抛异常")
    void handlesNull() {
        assertNull(PhoneMask.mask(null));
    }

    @Test
    @DisplayName("脱敏结果不包含完整原号码")
    void resultDoesNotLeakFullNumber() {
        String phone = "13912345678";
        String masked = PhoneMask.mask(phone);
        assertNotEquals(phone, masked);
        assertFalse(masked.contains(phone));
        // 中间 4 位必须被隐去
        assertFalse(masked.contains("1234"));
    }
}
