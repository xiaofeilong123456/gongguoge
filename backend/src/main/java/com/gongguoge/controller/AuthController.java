package com.gongguoge.controller;

import com.gongguoge.common.Result;
import com.gongguoge.common.util.UserContext;
import com.gongguoge.dto.ChangePasswordRequest;
import com.gongguoge.dto.LoginRequest;
import com.gongguoge.dto.RegisterRequest;
import com.gongguoge.dto.SmsCodeRequest;
import com.gongguoge.service.AuthService;
import com.gongguoge.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sms-code")
    public Result<Void> sendSmsCode(@Valid @RequestBody SmsCodeRequest request,
                                    HttpServletRequest httpRequest) {
        // 传入客户端 IP 做「按 IP 限流」，防止有人用不同手机号批量刷短信产生费用
        authService.sendSmsCode(request, resolveClientIp(httpRequest));
        return Result.success("验证码已发送", null);
    }

    /**
     * 取真实客户端 IP。
     *
     * <p>上线后若前面挂了 Nginx，直连地址会是 127.0.0.1，
     * 此时必须读 X-Forwarded-For（Nginx 配置里已设置该头）。</p>
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            // 形如 "客户端IP, 代理1, 代理2"，取第一个
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success("注册成功", authService.register(request));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success("登录成功", authService.login(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        // JWT 无状态，前端删除本地 token 即可
        return Result.success();
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request, UserContext.getUserId());
        return Result.success();
    }
}
