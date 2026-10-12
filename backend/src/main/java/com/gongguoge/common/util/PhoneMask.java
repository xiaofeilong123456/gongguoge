package com.gongguoge.common.util;

/**
 * 手机号脱敏。
 *
 * <p>手机号属于个人信息，生产日志中不应明文留存（《个人信息保护法》要求最小必要）。
 * 所有涉及手机号的日志都走这里，避免遗漏。</p>
 *
 * <p>示例：{@code 13800001234 -> 138****1234}</p>
 */
public final class PhoneMask {

    private PhoneMask() {
    }

    public static String mask(String phone) {
        if (phone == null) {
            return null;
        }
        String p = phone.trim();
        if (p.length() < 7) {
            // 太短，无法保留前后段，整体隐去
            return "***";
        }
        return p.substring(0, 3) + "****" + p.substring(p.length() - 4);
    }
}
