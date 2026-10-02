package com.buguagaoshu.food.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.ReviewService;
import com.buguagaoshu.food.utils.StoreAuthHelper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 评价控制器
 * <p>
 * - 创建评价：顾客本人已完成订单
 * - 回复评价：店家（review:manage 权限）
 * - 门店评价列表：公开
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-02
 */
@RestController
@RequestMapping("/api/review")
@Validated
public class ReviewController {

    private final ReviewService reviewService;
    private final StoreAuthHelper storeAuthHelper;

    public ReviewController(ReviewService reviewService, StoreAuthHelper storeAuthHelper) {
        this.reviewService = reviewService;
        this.storeAuthHelper = storeAuthHelper;
    }

    /**
     * 创建评价（订单完成后）
     */
    @SaCheckLogin
    @PostMapping("/add")
    public ResponseDetails createReview(@RequestBody Map<String, Object> params) {
        Long orderId = Long.valueOf(String.valueOf(params.get("orderId")));
        Integer rating = Integer.valueOf(String.valueOf(params.get("rating")));
        String content = params.get("content") == null ? null : String.valueOf(params.get("content"));
        return ResponseDetails.ok().put("data", reviewService.createReview(orderId, rating, content));
    }

    /**
     * 商家回复评价
     */
    @SaCheckLogin
    @PostMapping("/reply")
    public ResponseDetails replyReview(@RequestParam @NotNull Long reviewId,
                                       @RequestParam @NotNull Long storeId,
                                       @RequestParam @NotNull String reply) {
        storeAuthHelper.checkStorePermission(storeId, "review:manage");
        return ResponseDetails.ok().put("data", reviewService.replyReview(reviewId, storeId, reply));
    }

    /**
     * 分页查询门店评价（公开）
     */
    @GetMapping("/store/{storeId}")
    public ResponseDetails listStoreReviews(
            @PathVariable Long storeId,
            @RequestParam(value = "page", required = false, defaultValue = "1")
            @Min(value = 1, message = "页码不能小于1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10")
            @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 100, message = "每页条数不能超过100") Integer size) {
        return ResponseDetails.ok().put("data", reviewService.listStoreReviews(storeId, page, size));
    }

    /**
     * 按订单查询评价状态（顾客端）
     */
    @SaCheckLogin
    @GetMapping("/order/{orderId}")
    public ResponseDetails findByOrder(@PathVariable Long orderId) {
        return ResponseDetails.ok().put("data", reviewService.findByOrder(orderId));
    }
}
