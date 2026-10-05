package com.buguagaoshu.food.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.data.Membership;
import com.buguagaoshu.food.data.UserData;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.MembershipService;
import com.buguagaoshu.food.service.OrderService;
import com.buguagaoshu.food.service.UserDataService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
    private final MembershipService membershipService;
    private final OrderService orderService;

    public UserController(UserDataService userDataService,
                          MembershipService membershipService,
                          OrderService orderService) {
        this.userDataService = userDataService;
        this.membershipService = membershipService;
        this.orderService = orderService;
    }

    /**
     * 用户注册
     * 注册成功后自动分配默认角色"顾客"(CUSTOMER)
     */
    @Valid
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

    /**
     * 查询当前登录用户在指定门店的会员信息（顾客端，无需 member:manage 权限）
     * <p>
     * 用于顾客下单/结算页自动同步会员信息：
     * <ul>
     *   <li>优先按当前用户档案手机号在本店查会员记录</li>
     *   <li>手机号为空时按 userId 关联（早期数据可能用 userId 关联）</li>
     *   <li>查无记录返回 membership=null，前端按「非会员」展示，下单后自动建档</li>
     * </ul>
     * 返回内容已脱敏：手机号中间四位用 **** 替换；不含任何敏感备注字段。
     *
     * @param storeId 门店ID
     */
    @SaCheckLogin
    @GetMapping("/me/membership")
    public ResponseDetails myMembership(@RequestParam Long storeId) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserData user = userDataService.findById(userId).orElse(null);
        String phone = user != null ? user.getPhone() : null;

        Membership m = null;
        if (phone != null && !phone.isBlank()) {
            m = membershipService.findByStoreAndPhone(storeId, phone);
        }
        if (m == null) {
            // 手机号未关联会员时，按 userId 兜底查（历史数据可能用 userId 直接关联）
            var list = membershipService.findByStoreIdAndUserId(storeId, userId);
            if (!list.isEmpty()) {
                m = list.get(0);
            }
        }

        Map<String, Object> result = new HashMap<>();
        // 用户档案手机号（脱敏），供前端展示与下单时自动回填
        result.put("phone", phone);
        result.put("maskedPhone", maskPhone(phone));

        if (m != null) {
            Map<String, Object> membership = new HashMap<>();
            membership.put("id", m.getId());
            membership.put("nickname", m.getNickname());
            membership.put("num", m.getNum() != null ? m.getNum() : 0L);
            // 用餐次数按手机号在本店的订单数实时统计（与店主端口径一致）
            if (phone != null && !phone.isBlank()) {
                membership.put("count", orderService.countStoreOrdersByPhone(storeId, phone));
            } else {
                membership.put("count", m.getCount() != null ? m.getCount() : 0L);
            }
            result.put("membership", membership);
        } else {
            result.put("membership", null);
        }
        return ResponseDetails.ok().put("data", result);
    }

    /** 手机号脱敏：138****8888，长度非 11 位则原样返回 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone == null ? "" : phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
