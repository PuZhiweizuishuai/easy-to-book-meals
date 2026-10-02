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

    public AuthService(UserDataService userDataService) {
        this.userDataService = userDataService;
    }

    /**
     * 通用登录：手机号或邮箱+密码
     *
     * @param username 手机号或邮箱
     * @param password 密码
     * @return 登录结果 {token, user}
     */
    public Map<String, Object> login(String username, String password) {
        if (!StringUtils.hasLength(username) || !StringUtils.hasLength(password)) {
            throw new BusinessException(400, "用户名和密码不能为空");
        }

        UserData user;
        if (username.contains("@")) {
            user = userDataService.findByEmail(username)
                    .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        } else {
            user = userDataService.findByPhone(username)
                    .orElseThrow(() -> new BusinessException(404, "用户不存在"));
        }

        if (user.getStatus() != WebConstant.DISABLE) {
            throw new BusinessException(403, "账号已被禁用");
        }

        if (!PasswordUtil.matches(password, user.getPassword())) {
            throw new BusinessException(400, "密码错误");
        }

        // 历史 MD5 密文在校验通过后透明升级为 BCrypt
        if (!PasswordUtil.isBCrypt(user.getPassword())) {
            userDataService.upgradePasswordToBCrypt(user.getId(), password);
        }

        StpUtil.login(user.getId());
        userDataService.fillUserRolesAndPermissions(user);
        user.setPassword(null);

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
