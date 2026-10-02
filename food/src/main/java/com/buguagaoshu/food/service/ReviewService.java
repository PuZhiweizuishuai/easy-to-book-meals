package com.buguagaoshu.food.service;

import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.data.OrderData;
import com.buguagaoshu.food.data.ReviewData;
import com.buguagaoshu.food.data.UserData;
import com.buguagaoshu.food.enums.OrderStatus;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.ReviewDataRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 评价服务
 * <p>
 * 评价规则：
 * 1. 仅已完成(COMPLETED)的订单可评价
 * 2. 只能评价自己的订单
 * 3. 一个订单只能评价一次
 * 4. 店家可回复本店评价（review:manage 权限，由控制器校验）
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-02
 */
@Service
public class ReviewService {

    private final ReviewDataRepository reviewDataRepository;
    private final OrderService orderService;
    private final UserDataService userDataService;

    public ReviewService(ReviewDataRepository reviewDataRepository,
                         OrderService orderService,
                         UserDataService userDataService) {
        this.reviewDataRepository = reviewDataRepository;
        this.orderService = orderService;
        this.userDataService = userDataService;
    }

    /**
     * 创建评价（顾客，订单完成后）
     */
    @Transactional(rollbackFor = Exception.class)
    public ReviewData createReview(Long orderId, Integer rating, String content) {
        Long userId = StpUtil.getLoginIdAsLong();

        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException(400, "评分必须为 1-5 星");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException(400, "评价内容不能为空");
        }

        // 校验订单：本人 + 已完成
        OrderData order = orderService.getOrderDetail(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "只能评价自己的订单");
        }
        if (!order.getStatus().equals(OrderStatus.COMPLETED.getCode())) {
            throw new BusinessException(400, "订单完成后才能评价");
        }
        if (reviewDataRepository.findByOrderId(orderId).isPresent()) {
            throw new BusinessException(400, "该订单已评价过");
        }

        ReviewData review = new ReviewData();
        review.setOrderId(orderId);
        review.setStoreId(order.getStoreId());
        review.setUserId(userId);
        UserData user = userDataService.findById(userId).orElse(null);
        review.setUserNickname(user != null ? user.getNickname() : null);
        review.setRating(rating);
        review.setContent(content.trim());
        review.setStatus(ReviewData.STATUS_NORMAL);
        return reviewDataRepository.save(review);
    }

    /**
     * 店家回复评价
     */
    @Transactional(rollbackFor = Exception.class)
    public ReviewData replyReview(Long reviewId, Long storeId, String reply) {
        if (reply == null || reply.isBlank()) {
            throw new BusinessException(400, "回复内容不能为空");
        }
        ReviewData review = reviewDataRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(404, "评价不存在"));
        if (!review.getStoreId().equals(storeId)) {
            throw new BusinessException(403, "只能回复本店评价");
        }

        review.setMerchantReply(reply.trim());
        review.setReplyTime(System.currentTimeMillis());
        return reviewDataRepository.save(review);
    }

    /**
     * 分页查询门店评价（公开，仅正常状态）
     */
    public Page<ReviewData> listStoreReviews(Long storeId, int page, int size) {
        return reviewDataRepository.findByStoreIdAndStatusOrderByCreateTimeDesc(
                storeId, ReviewData.STATUS_NORMAL, PageRequest.of(page - 1, size));
    }

    /**
     * 按订单查询评价（顾客查看自己的订单是否已评价）
     */
    public Map<String, Object> findByOrder(Long orderId) {
        ReviewData review = reviewDataRepository.findByOrderId(orderId).orElse(null);
        Map<String, Object> result = new HashMap<>();
        result.put("reviewed", review != null);
        result.put("review", review);
        return result;
    }
}
