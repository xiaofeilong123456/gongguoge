package com.gongguoge.common.util;

import com.gongguoge.pojo.User;

public class UserContext {

    private static final ThreadLocal<User> userHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> tokenHolder = new ThreadLocal<>();

    public static void setUser(User user) {
        userHolder.set(user);
    }

    public static User getUser() {
        return userHolder.get();
    }

    public static Long getUserId() {
        User user = userHolder.get();
        return user == null ? null : user.getId();
    }

    public static void setToken(String token) {
        tokenHolder.set(token);
    }

    public static String getToken() {
        return tokenHolder.get();
    }

    public static void clear() {
        userHolder.remove();
        tokenHolder.remove();
    }
}
