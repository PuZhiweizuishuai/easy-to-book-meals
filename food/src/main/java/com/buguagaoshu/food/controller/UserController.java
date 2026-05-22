package com.buguagaoshu.food.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.buguagaoshu.food.data.UserData;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.UserDataService;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 提供用户注册、信息修改、查询等面向普通用户的功能
 * 系统级管理功能（角色分配、状态管理）在 controller.sys 包下
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-21
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserDataService userDataService;

    public UserController(UserDataService userDataService) {
        this.userDataService = userDataService;
    }

    /**
     * 用户注册
     * 注册成功后自动分配默认角色"顾客"(CUSTOMER)
     */
    @PostMapping("/register")
    public ResponseDetails register(@RequestBody UserData userData) {
        UserData savedUser = userDataService.register(userData);
        savedUser.setPassword(null);
        return ResponseDetails.ok().put("data", savedUser);
    }

    /**
     * 修改用户信息
     * 禁止修改系统级参数：status、角色信息、createTime
     */
    @SaCheckLogin
    @PostMapping("/update")
    public ResponseDetails updateUserInfo(@RequestBody UserData userData) {
        UserData updatedUser = userDataService.updateUserInfo(userData);
        updatedUser.setPassword(null);
        return ResponseDetails.ok().put("data", updatedUser);
    }

    /**
     * 根据ID查询用户信息（含角色与权限）
     */
    @SaCheckLogin
    @GetMapping("/{id}")
    public ResponseDetails getUserById(@PathVariable Long id) {
        UserData user = userDataService.findUserWithRolesAndPermissions(id);
        user.setPassword(null);
        return ResponseDetails.ok().put("data", user);
    }
}
