package com.buguagaoshu.food.controller.sys;

import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.data.UserData;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.service.UserDataService;
import com.buguagaoshu.food.utils.MD5Util;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证管理控制器
 * 提供登录、登出、获取当前用户信息等认证相关功能
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-22
 */
@RestController
@RequestMapping("/api/sys/auth")
public class AuthSysController {

    private final UserDataService userDataService;

    public AuthSysController(UserDataService userDataService) {
        this.userDataService = userDataService;
    }

    /**
     * 手机号+密码登录
     */
    @PostMapping("/login/phone")
    public ResponseDetails loginByPhone(@RequestBody Map<String, String> params) {
        String phone = params.get("phone");
        String password = params.get("password");

        if (!StringUtils.hasLength(phone) || !StringUtils.hasLength(password)) {
            throw new BusinessException(400, "手机号和密码不能为空");
        }

        UserData user = userDataService.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        if (user.getStatus() != 0) {
            throw new BusinessException(403, "账号已被禁用");
        }

        if (!MD5Util.getMD5(password).equals(user.getPassword())) {
            throw new BusinessException(400, "密码错误");
        }

        StpUtil.login(user.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("userId", user.getId());
        result.put("nickname", user.getNickname());
        return ResponseDetails.ok().put("data", result);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public ResponseDetails logout() {
        StpUtil.logout();
        return ResponseDetails.ok().put("message", "登出成功");
    }

    /**
     * 获取当前登录用户信息（含角色与权限）
     */
    @GetMapping("/info")
    public ResponseDetails getCurrentUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserData user = userDataService.findUserWithRolesAndPermissions(userId);
        user.setPassword(null);
        return ResponseDetails.ok().put("data", user);
    }
}
