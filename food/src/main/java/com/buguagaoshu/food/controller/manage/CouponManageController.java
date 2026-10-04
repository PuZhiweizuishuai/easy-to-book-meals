package com.buguagaoshu.food.controller.manage;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.buguagaoshu.food.data.Coupon;
import com.buguagaoshu.food.data.UserCoupon;
import com.buguagaoshu.food.dto.CreateCouponDto;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.CouponService;
import com.buguagaoshu.food.utils.StoreAuthHelper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 优惠券管理控制器（店主端）
 * <p>
 * 提供券模板的创建、列表、详情、启停，以及券码列表（供前端生成二维码）。
 * 查询需要 coupon:view 权限，创建/启停需要 coupon:manage 权限。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-04
 */
@RestController
@RequestMapping("/api/manage/coupon")
@Validated
public class CouponManageController {

    private final CouponService couponService;
    private final StoreAuthHelper storeAuthHelper;

    public CouponManageController(CouponService couponService, StoreAuthHelper storeAuthHelper) {
        this.couponService = couponService;
        this.storeAuthHelper = storeAuthHelper;
    }

    /**
     * 券模板分页列表
     */
    @SaCheckLogin
    @GetMapping("/list")
    public ResponseDetails list(@RequestParam @NotNull(message = "门店ID不能为空") Long storeId,
                                @RequestParam(required = false) String key,
                                @RequestParam(defaultValue = "1")
                                @Min(value = 1, message = "页码不能小于1") Integer page,
                                @RequestParam(defaultValue = "10")
                                @Min(value = 1, message = "每页条数不能小于1")
                                @Max(value = 100, message = "每页条数不能超过100") Integer size) {
        storeAuthHelper.checkStoreAnyPermission(storeId, "coupon:manage", "coupon:view");
        Page<Coupon> coupons = couponService.listCoupons(storeId, key, page, size);
        return ResponseDetails.ok().put("data", coupons);
    }

    /**
     * 券模板详情
     */
    @SaCheckLogin
    @GetMapping("/{id}")
    public ResponseDetails detail(@PathVariable Long id,
                                  @RequestParam @NotNull(message = "门店ID不能为空") Long storeId) {
        storeAuthHelper.checkStoreAnyPermission(storeId, "coupon:manage", "coupon:view");
        return ResponseDetails.ok().put("data", couponService.getCoupon(id, storeId));
    }

    /**
     * 创建券模板（同时批量生成券码）
     */
    @SaCheckLogin
    @PostMapping("/add")
    public ResponseDetails add(@Valid @RequestBody CreateCouponDto dto) {
        storeAuthHelper.checkStorePermission(dto.getStoreId(), "coupon:manage");
        return ResponseDetails.ok().put("data", couponService.createCoupon(dto));
    }

    /**
     * 启停券模板（停用时作废该券未使用的券码）
     */
    @SaCheckLogin
    @PostMapping("/status")
    public ResponseDetails updateStatus(@RequestParam @NotNull(message = "门店ID不能为空") Long storeId,
                                        @RequestParam @NotNull(message = "券ID不能为空") Long couponId,
                                        @RequestParam @NotNull(message = "状态不能为空") Integer status) {
        storeAuthHelper.checkStorePermission(storeId, "coupon:manage");
        return ResponseDetails.ok().put("data", couponService.updateStatus(couponId, storeId, status));
    }

    /**
     * 券码列表（分页，附带券模板信息，前端据此生成二维码）
     */
    @SaCheckLogin
    @GetMapping("/{id}/codes")
    public ResponseDetails codes(@PathVariable Long id,
                                 @RequestParam @NotNull(message = "门店ID不能为空") Long storeId,
                                 @RequestParam(required = false) Integer status,
                                 @RequestParam(defaultValue = "1")
                                 @Min(value = 1, message = "页码不能小于1") Integer page,
                                 @RequestParam(defaultValue = "20")
                                 @Min(value = 1, message = "每页条数不能小于1")
                                 @Max(value = 100, message = "每页条数不能超过100") Integer size) {
        storeAuthHelper.checkStoreAnyPermission(storeId, "coupon:manage", "coupon:view");
        Page<UserCoupon> codes = couponService.listCodes(id, storeId, status, page, size);
        return ResponseDetails.ok().put("data", codes);
    }

    /**
     * 店员按手机号查询该会员名下可用优惠券（结算时展示）
     *
     * @param amount 当前订单金额，用于试算每张券可优惠金额
     */
    @SaCheckLogin
    @GetMapping("/member-coupons")
    public ResponseDetails memberCoupons(@RequestParam @NotNull(message = "门店ID不能为空") Long storeId,
                                         @RequestParam @NotNull(message = "手机号不能为空") String phone,
                                         @RequestParam(required = false) BigDecimal amount) {
        storeAuthHelper.checkStorePermission(storeId, "order:manage");
        Map<String, Object> result = new HashMap<>();
        result.put("coupons", couponService.memberAvailableCoupons(storeId, phone.trim(), amount));
        return ResponseDetails.ok().put("data", result);
    }
}
