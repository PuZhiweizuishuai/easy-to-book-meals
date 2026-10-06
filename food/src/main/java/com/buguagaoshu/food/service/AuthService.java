package com.buguagaoshu.food.service;

import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.config.WebConstant;
import com.buguagaoshu.food.data.UserData;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.utils.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 认证服务
 * 处理登录、登出、用户信息查询等认证相关业务逻辑
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-27
 */
@Service
public class AuthService {
    private final UserDataService userDataService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthService(UserDataService userDataService, LoginRateLimiter loginRateLimiter) {
        this.userDataService = userDataService;
        this.loginRateLimiter = loginRateLimiter;
    }

    /**
     * 通用登录：手机号或邮箱+密码
     * <p>
     * 登录失败限流（账号 + IP 双维度，阈值与锁定时长由超级后台系统设置配置）：
     * <ol>
     *   <li>先检查账号 / IP 是否处于锁定中，锁定则直接拒绝；</li>
     *   <li>账号不存在只计入 IP 失败；密码错误账号、IP 各计一次；</li>
     *   <li>登录成功清除失败计数。</li>
     * </ol>
     *
     * @param username 手机号或邮箱
     * @param password 密码
     * @param clientIp 客户端 IP
     * @return 登录结果 {token, user}
     */
    public Map<String, Object> login(String username, String password, String clientIp) {
        if (!StringUtils.hasLength(username) || !StringUtils.hasLength(password)) {
            throw new BusinessException(400, "用户名和密码不能为空");
        }

        // 锁定中的账号 / IP 直接拒绝，不做密码校验
        loginRateLimiter.assertLoginAllowed(username, clientIp);

        Optional<UserData> userOptional;
        if (username.contains("@")) {
            userOptional = userDataService.findByEmail(username);
        } else {
            userOptional = userDataService.findByPhone(username);
        }

        // 账号不存在：只计 IP 失败（防止用登录接口枚举 / 撞库大量用户名）
        if (userOptional.isEmpty()) {
            loginRateLimiter.recordLoginFailure(username, clientIp, false);
            throw new BusinessException(404, "用户不存在");
        }

        UserData user = userOptional.get();

        if (user.getStatus() != WebConstant.DISABLE) {
            throw new BusinessException(403, "账号已被禁用");
        }

        if (!PasswordUtil.matches(password, user.getPassword())) {
            // 密码错误：账号、IP 双维度各计一次
            loginRateLimiter.recordLoginFailure(username, clientIp, true);
            throw new BusinessException(400, "密码错误");
        }

        // 历史 MD5 密文在校验通过后透明升级为 BCrypt
        if (!PasswordUtil.isBCrypt(user.getPassword())) {
            userDataService.upgradePasswordToBCrypt(user.getId(), password);
        }

        StpUtil.login(user.getId());
        userDataService.fillUserRolesAndPermissions(user);
        user.setPassword(null);

        // 登录成功：清除该账号与 IP 的失败计数
        loginRateLimiter.recordLoginSuccess(username, clientIp);

        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("user", user);
        return result;
    }

    /**
     * 登出
     */
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 获取当前登录用户信息（含角色与权限）
     */
    public UserData getCurrentUserInfo() {
        Long userId = StpUtil.getLoginIdAsLong();
        UserData user = userDataService.findUserWithRolesAndPermissions(userId);
        user.setPassword(null);
        return user;
    }
}
