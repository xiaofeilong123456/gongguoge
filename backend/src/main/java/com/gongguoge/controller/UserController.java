package com.gongguoge.controller;

import com.gongguoge.common.Result;
import com.gongguoge.common.util.UserContext;
import com.gongguoge.dto.ProfileUpdateRequest;
import com.gongguoge.service.UserService;
import com.gongguoge.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public Result<UserVO> profile() {
        return Result.success(userService.getProfile(UserContext.getUserId()));
    }

    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return Result.success("资料已更新", userService.updateProfile(request, UserContext.getUserId()));
    }

    /**
     * 注销账号（不可恢复）。
     * 删除账号本身及其全部个人数据；调用后当前 token 立即失效。
     */
    @DeleteMapping("/account")
    public Result<Void> deleteAccount() {
        userService.deleteAccount(UserContext.getUserId());
        return Result.success("账号已注销", null);
    }
}
