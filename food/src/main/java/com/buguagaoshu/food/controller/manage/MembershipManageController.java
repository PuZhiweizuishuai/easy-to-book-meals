package com.buguagaoshu.food.controller.manage;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.buguagaoshu.food.data.Membership;
import com.buguagaoshu.food.data.OrderData;
import com.buguagaoshu.food.data.PointsTransaction;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.service.MembershipService;
import com.buguagaoshu.food.service.OrderService;
import com.buguagaoshu.food.utils.StoreAuthHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 会员管理控制器（店主端）
 * <p>
 * 提供门店会员列表、积分流水、关联订单查询及手动调整积分功能。
 * 会员统一通过手机号关联，未注册用户也能按手机号累积积分。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-03
 */
@RestController
@RequestMapping("/api/manage/membership")
public class MembershipManageController {

    private final MembershipService membershipService;
    private final OrderService orderService;
    private final StoreAuthHelper storeAuthHelper;

    public MembershipManageController(MembershipService membershipService,
                                     OrderService orderService,
                                     StoreAuthHelper storeAuthHelper) {
        this.membershipService = membershipService;
        this.orderService = orderService;
        this.storeAuthHelper = storeAuthHelper;
    }

    /**
     * 会员列表（支持按手机号搜索）
     */
    @SaCheckLogin
    @GetMapping("/list")
    public ResponseDetails listMembers(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String key) {
        storeAuthHelper.checkStorePermission(storeId, "member:manage");
        PageRequest pageRequest = PageRequest.of(Math.max(page - 1, 0), size,
                Sort.by(Sort.Direction.DESC, "updateTime"));
        Page<Membership> members = membershipService.findStoreMembers(storeId, key, pageRequest);
        // 用餐次数直接取该手机号在本店的消费记录数量，保证与消费记录一致
        members.forEach(m -> m.setCount(orderService.countStoreOrdersByPhone(storeId, m.getPhone())));
        return ResponseDetails.ok().put("data", members);
    }

    /**
     * 会员详情
     */
    @SaCheckLogin
    @GetMapping("/{id}")
    public ResponseDetails memberDetail(@PathVariable Long id, @RequestParam Long storeId) {
        storeAuthHelper.checkStorePermission(storeId, "member:manage");
        Membership m = getMemberOrThrow(id, storeId);
        m.setCount(orderService.countStoreOrdersByPhone(storeId, m.getPhone()));
        return ResponseDetails.ok().put("data", m);
    }

    /**
     * 会员积分流水（分页，按手机号统一查询）
     */
    @SaCheckLogin
    @GetMapping("/{id}/transactions")
    public ResponseDetails memberTransactions(
            @PathVariable Long id,
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        storeAuthHelper.checkStorePermission(storeId, "member:manage");
        Membership m = getMemberOrThrow(id, storeId);
        PageRequest pageRequest = PageRequest.of(Math.max(page - 1, 0), size,
                Sort.by(Sort.Direction.DESC, "createTime"));
        Page<PointsTransaction> txs = membershipService
                .findTransactionsByStoreAndPhone(storeId, m.getPhone(), pageRequest);
        return ResponseDetails.ok().put("data", txs);
    }

    /**
     * 会员关联订单（按手机号查询消费记录）
     */
    @SaCheckLogin
    @GetMapping("/{id}/orders")
    public ResponseDetails memberOrders(
            @PathVariable Long id,
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        storeAuthHelper.checkStorePermission(storeId, "member:manage");
        Membership m = getMemberOrThrow(id, storeId);
        PageRequest pageRequest = PageRequest.of(Math.max(page - 1, 0), size,
                Sort.by(Sort.Direction.DESC, "createTime"));
        Page<OrderData> orders = orderService.findStoreOrdersByPhone(storeId, m.getPhone(), pageRequest);
        return ResponseDetails.ok().put("data", orders);
    }

    /**
     * 手动调整会员积分（正数增加，负数扣减），并记录流水。
     */
    @SaCheckLogin
    @PostMapping("/adjust")
    public ResponseDetails adjustPoints(
            @RequestParam Long storeId,
            @RequestParam Long membershipId,
            @RequestParam Long points,
            @RequestParam(required = false) String remark) {
        storeAuthHelper.checkStorePermission(storeId, "member:manage");
        Membership m = getMemberOrThrow(membershipId, storeId);
        Membership updated = membershipService.adjustPoints(storeId, m.getPhone(), points, remark);
        Map<String, Object> result = new HashMap<>();
        result.put("membership", updated);
        result.put("message", "积分调整成功");
        return ResponseDetails.ok().put("data", result);
    }

    private Membership getMemberOrThrow(Long id, Long storeId) {
        Membership m = membershipService.findByIdAndStoreId(id, storeId);
        if (m == null) {
            throw new BusinessException(404, "会员不存在");
        }
        return m;
    }
}
