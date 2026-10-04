package com.buguagaoshu.food.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.data.UserCoupon;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.CouponService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券控制器（顾客端）
 * <p>
 * - 券码详情：公开（未登录顾客出示券码/二维码即可查询券面信息）
 * - 领取：需登录，领取后券码关联到用户账户
 * - 我的券：需登录
 * - 校验试算：需登录，结算前试算优惠金额（不核销）
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-04
 */
@RestController
@RequestMapping("/api/coupon")
@Validated
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    /**
     * 按券码查询券详情（公开，未登录可展示券面信息）
     */
    @GetMapping("/detail/{code}")
    public ResponseDetails detail(@PathVariable String code) {
        return ResponseDetails.ok().put("data", couponService.findByCode(code));
    }

    /**
     * 领取优惠券（关联到当前登录用户）
     */
    @SaCheckLogin
    @PostMapping("/claim/{code}")
    public ResponseDetails claim(@PathVariable String code,
                                 @RequestParam(required = false) String phone) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserCoupon uc = couponService.claim(code, userId, phone);
        return ResponseDetails.ok().put("data", uc);
    }

    /**
     * 我的优惠券（指定门店）
     */
    @SaCheckLogin
    @GetMapping("/my")
    public ResponseDetails my(@RequestParam @NotNull(message = "门店ID不能为空") Long storeId) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<UserCoupon> list = couponService.myCoupons(storeId, userId);
        return ResponseDetails.ok().put("data", list);
    }

    /**
     * 校验券码在当前订单金额下是否可用（仅试算优惠金额，不核销）
     */
    @SaCheckLogin
    @GetMapping("/check")
    public ResponseDetails check(@RequestParam @NotNull(message = "门店ID不能为空") Long storeId,
                                 @RequestParam @NotNull(message = "优惠券码不能为空") String code,
                                 @RequestParam @NotNull(message = "订单金额不能为空")
                                 @DecimalMin(value = "0", message = "订单金额不能为负数") BigDecimal amount) {
        return ResponseDetails.ok().put("data", couponService.check(storeId, code, amount));
    }
}
