package com.buguagaoshu.food.controller;


import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证管理控制器
 * 提供登录、登出、获取当前用户信息等认证相关功能
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-22
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 通用登录接口 手机号或邮箱+密码登录（超管/店主/普通用户均可使用）
     */
    @PostMapping("/login")
    public ResponseDetails login(@RequestBody Map<String, String> params) {
        return ResponseDetails.ok().put("data",
                authService.login(
                        params.get("username"),
                        params.get("password")));
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public ResponseDetails logout() {
        authService.logout();
        return ResponseDetails.ok().put("data", "登出成功");
    }

    /**
     * 获取当前登录用户信息（含角色与权限）
     */
    @GetMapping("/info")
    public ResponseDetails getCurrentUserInfo() {
        return ResponseDetails.ok().put("data", authService.getCurrentUserInfo());
    }
}
