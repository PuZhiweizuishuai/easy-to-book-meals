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
 * <p>
 * 登录入口：
 * - /api/sys/auth/login/phone   手机号+密码登录（通用，所有角色可用）
 * - /api/sys/auth/login/email   邮箱+密码登录（通用，所有角色可用）
 * - /api/sys/auth/login/wx      微信OpenID登录（小程序/公众号）
 * - /api/sys/auth/logout        登出
 * - /api/sys/auth/info          获取当前用户信息
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
     * 手机号+密码登录（通用，超管/店主/普通用户均可使用）
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

        return doLogin(user, password);
    }

    /**
     * 邮箱+密码登录（通用，超管/店主/普通用户均可使用）
     */
    @PostMapping("/login/email")
    public ResponseDetails loginByEmail(@RequestBody Map<String, String> params) {
        String email = params.get("email");
        String password = params.get("password");

        if (!StringUtils.hasLength(email) || !StringUtils.hasLength(password)) {
            throw new BusinessException(400, "邮箱和密码不能为空");
        }

        UserData user = userDataService.findByEmail(email)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));

        return doLogin(user, password);
    }

    /**
     * 微信OpenID登录（小程序/公众号免密登录）
     * 若用户不存在，需先通过注册接口创建账号
     */
    @PostMapping("/login/wx")
    public ResponseDetails loginByWx(@RequestBody Map<String, String> params) {
        String wxOpenid = params.get("wxOpenid");

        if (!StringUtils.hasLength(wxOpenid)) {
            throw new BusinessException(400, "微信OpenID不能为空");
        }

        UserData user = userDataService.findByWxOpenid(wxOpenid)
                .orElseThrow(() -> new BusinessException(404, "微信账号未绑定，请先注册"));

        if (user.getStatus() != 0) {
            throw new BusinessException(403, "账号已被禁用");
        }

        StpUtil.login(user.getId());
        return buildLoginResult(user);
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

    private ResponseDetails doLogin(UserData user, String password) {
        if (user.getStatus() != 0) {
            throw new BusinessException(403, "账号已被禁用");
        }

        if (!MD5Util.getMD5(password).equals(user.getPassword())) {
            throw new BusinessException(400, "密码错误");
        }

        StpUtil.login(user.getId());
        return buildLoginResult(user);
    }

    private ResponseDetails buildLoginResult(UserData user) {
        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("userId", user.getId());
        result.put("nickname", user.getNickname());
        return ResponseDetails.ok().put("data", result);
    }
}
