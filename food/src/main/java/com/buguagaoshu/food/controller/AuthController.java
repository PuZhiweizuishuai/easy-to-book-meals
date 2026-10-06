package com.buguagaoshu.food.controller;


import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
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
     * <p>
     * 解析客户端真实 IP 用于登录失败限流；经过 Nginx 等反向代理时依赖代理设置
     * X-Forwarded-For / X-Real-IP 头（生产环境应禁止外部客户端直接伪造这些头）。
     */
    @PostMapping("/login")
    public ResponseDetails login(@RequestBody Map<String, String> params, HttpServletRequest request) {
        return ResponseDetails.ok().put("data",
                authService.login(
                        params.get("username"),
                        params.get("password"),
                        getClientIp(request)));
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

    /**
     * 获取客户端 IP：优先 X-Forwarded-For 首个地址，其次 X-Real-IP，最后直连地址
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            // 多级代理时第一个为最初客户端地址
            for (String part : xff.split(",")) {
                String ip = part.trim();
                if (!ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                    return ip;
                }
            }
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp) && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
