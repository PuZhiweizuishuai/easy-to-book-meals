package com.buguagaoshu.food.controller.manage;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.buguagaoshu.food.data.OrderData;
import com.buguagaoshu.food.dto.AddOrderItemsDto;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.dto.StaffCreateOrderDto;
import com.buguagaoshu.food.enums.OrderStatus;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.service.OrderService;
import com.buguagaoshu.food.utils.StoreAuthHelper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单管理控制器（店主/店员端）
 * <p>
 * 提供门店订单的查询与处理（接单/拒单/制作/出餐/完成）、店员协助下单、加菜
 * 查询需要 order:view 权限，处理需要 order:manage 权限
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-02
 */
@RestController
@RequestMapping("/api/order/manage")
@Validated
public class OrderManageController {

    private final OrderService orderService;
    private final StoreAuthHelper storeAuthHelper;

    public OrderManageController(OrderService orderService, StoreAuthHelper storeAuthHelper) {
        this.orderService = orderService;
        this.storeAuthHelper = storeAuthHelper;
    }

    /**
     * 分页查询门店订单（可按状态、订单类型过滤，订单号搜索）
     */
    @SaCheckLogin
    @GetMapping("/list")
    public ResponseDetails list(
            @RequestParam @NotNull(message = "门店ID不能为空") Long storeId,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "orderType", required = false) Integer orderType,
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "page", required = false, defaultValue = "1")
            @Min(value = 1, message = "页码不能小于1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10")
            @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 100, message = "每页条数不能超过100") Integer size) {
        storeAuthHelper.checkStorePermission(storeId, "order:view");
        return ResponseDetails.ok().put("data",
                orderService.listStoreOrders(storeId, status, orderType, key, page, size));
    }

    /**
     * 订单详情
     */
    @SaCheckLogin
    @GetMapping("/detail/{id}")
    public ResponseDetails detail(@PathVariable Long id,
                                  @RequestParam @NotNull(message = "门店ID不能为空") Long storeId) {
        storeAuthHelper.checkStorePermission(storeId, "order:view");
        OrderData order = orderService.getOrderDetail(id);
        if (!order.getStoreId().equals(storeId)) {
            throw new BusinessException(403, "订单不属于该门店");
        }
        return ResponseDetails.ok().put("data", order);
    }

    /**
     * 接单
     */
    @SaCheckLogin
    @PostMapping("/confirm/{id}")
    public ResponseDetails confirm(@PathVariable Long id,
                                   @RequestParam @NotNull(message = "门店ID不能为空") Long storeId) {
        storeAuthHelper.checkStorePermission(storeId, "order:manage");
        return ResponseDetails.ok().put("data",
                orderService.transitionOrder(id, storeId, OrderStatus.CONFIRMED, null));
    }

    /**
     * 拒单（取消订单并恢复库存）
     */
    @SaCheckLogin
    @PostMapping("/reject/{id}")
    public ResponseDetails reject(@PathVariable Long id,
                                  @RequestParam @NotNull(message = "门店ID不能为空") Long storeId,
                                  @RequestParam(value = "reason", required = false) String reason) {
        storeAuthHelper.checkStorePermission(storeId, "order:manage");
        return ResponseDetails.ok().put("data",
                orderService.transitionOrder(id, storeId, OrderStatus.CANCELLED,
                        reason == null || reason.isBlank() ? "店家拒单" : reason));
    }

    /**
     * 开始制作
     */
    @SaCheckLogin
    @PostMapping("/cooking/{id}")
    public ResponseDetails cooking(@PathVariable Long id,
                                   @RequestParam @NotNull(message = "门店ID不能为空") Long storeId) {
        storeAuthHelper.checkStorePermission(storeId, "order:manage");
        return ResponseDetails.ok().put("data",
                orderService.transitionOrder(id, storeId, OrderStatus.COOKING, null));
    }

    /**
     * 出餐
     */
    @SaCheckLogin
    @PostMapping("/ready/{id}")
    public ResponseDetails ready(@PathVariable Long id,
                                 @RequestParam @NotNull(message = "门店ID不能为空") Long storeId) {
        storeAuthHelper.checkStorePermission(storeId, "order:manage");
        return ResponseDetails.ok().put("data",
                orderService.transitionOrder(id, storeId, OrderStatus.READY, null));
    }

    /**
     * 完成订单（结算）
     * <p>
     * 可携带顾客手机号：录入后会把订单归属关联到该手机号对应的会员，
     * 并按实际应付金额累积会员积分（1 元 = 1 积分）。
     * 手机号为空时按普通结算处理（匿名堂食订单常见）。
     * <p>
     * 结算金额构成：菜品折后合计 − 优惠券减免 − 手动减免 = 应付金额。
     * 券码可为空（不使用券）；manualDiscount 为店员手动录入的抹零/议价金额。
     */
    @SaCheckLogin
    @PostMapping("/complete/{id}")
    public ResponseDetails complete(@PathVariable Long id,
                                    @RequestParam @NotNull(message = "门店ID不能为空") Long storeId,
                                    @RequestParam(value = "phone", required = false) String phone,
                                    @RequestParam(value = "couponCode", required = false) String couponCode,
                                    @RequestParam(value = "manualDiscount", required = false)
                                    @DecimalMin(value = "0", message = "手动减免金额不能为负数") BigDecimal manualDiscount) {
        storeAuthHelper.checkStorePermission(storeId, "order:manage");
        return ResponseDetails.ok().put("data",
                orderService.completeOrder(id, storeId, phone, couponCode, manualDiscount));
    }

    /**
     * 店员协助下单（代客下单）
     * <p>
     * 店主或服务员在后台为顾客下单，线下餐桌订单需绑定餐桌。
     * 需要 order:manage 权限。
     */
    @SaCheckLogin
    @PostMapping("/staff-create")
    public ResponseDetails staffCreate(@Valid @RequestBody StaffCreateOrderDto dto) {
        storeAuthHelper.checkStorePermission(dto.getStoreId(), "order:manage");
        Long operatorId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        return ResponseDetails.ok().put("data", orderService.createStaffOrder(dto, operatorId));
    }

    /**
     * 店员为订单加菜（未结算前追加菜品）
     */
    @SaCheckLogin
    @PostMapping("/add-items/{id}")
    public ResponseDetails addItems(@PathVariable Long id,
                                    @RequestParam @NotNull(message = "门店ID不能为空") Long storeId,
                                    @Valid @RequestBody AddOrderItemsDto dto) {
        storeAuthHelper.checkStorePermission(storeId, "order:manage");
        OrderData order = orderService.getOrderDetail(id);
        if (!order.getStoreId().equals(storeId)) {
            throw new BusinessException(403, "订单不属于该门店");
        }
        return ResponseDetails.ok().put("data", orderService.addItemsToOrder(id, dto));
    }

    /**
     * 出餐（菜品级标记）
     * <p>
     * 店员/后厨逐个菜品确认出餐，当订单所有正常菜品均出餐后自动推进订单到「已出餐」状态。
     * 前置条件：订单必须已接单（待接单订单不允许出餐）。
     * <p>
     * 权限：门店订单管理（order:manage）或后厨出餐（kitchen:manage）。
     */
    @SaCheckLogin
    @PostMapping("/serve-item/{itemId}")
    public ResponseDetails serveItem(@PathVariable Long itemId,
                                     @RequestParam @NotNull(message = "门店ID不能为空") Long storeId) {
        storeAuthHelper.checkStoreAnyPermission(storeId, "order:manage", "kitchen:manage");
        return ResponseDetails.ok().put("data", orderService.serveOrderItem(itemId, storeId));
    }

    /**
     * 整单出餐：一次性把订单内所有未出餐菜品标记为已出餐（后厨批量完成）
     */
    @SaCheckLogin
    @PostMapping("/serve-order/{id}")
    public ResponseDetails serveOrder(@PathVariable Long id,
                                      @RequestParam @NotNull(message = "门店ID不能为空") Long storeId) {
        storeAuthHelper.checkStoreAnyPermission(storeId, "order:manage", "kitchen:manage");
        return ResponseDetails.ok().put("data", orderService.serveAllOrderItems(id, storeId));
    }

    /**
     * 开始制作（认领菜品）
     * <p>
     * 后厨人员点击后，该菜品从其他人的待制作列表中消失，仅在认领人列表中显示为「制作中」。
     */
    @SaCheckLogin
    @PostMapping("/start-preparing/{itemId}")
    public ResponseDetails startPreparing(@PathVariable Long itemId,
                                          @RequestParam @NotNull(message = "门店ID不能为空") Long storeId) {
        storeAuthHelper.checkStoreAnyPermission(storeId, "order:manage", "kitchen:manage");
        Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        return ResponseDetails.ok().put("data", orderService.startPreparingItem(itemId, storeId, userId));
    }

    /**
     * 释放制作（取消认领，菜品退回待制作队列）
     */
    @SaCheckLogin
    @PostMapping("/release-preparing/{itemId}")
    public ResponseDetails releasePreparing(@PathVariable Long itemId,
                                            @RequestParam @NotNull(message = "门店ID不能为空") Long storeId) {
        storeAuthHelper.checkStoreAnyPermission(storeId, "order:manage", "kitchen:manage");
        Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        orderService.releasePreparingItem(itemId, storeId, userId);
        return ResponseDetails.ok().put("message", "已释放");
    }

    /**
     * 后厨待制作队列（KDS）
     * <p>
     * 返回已接单且尚未出餐的<b>单个菜品</b>扁平列表，按菜品下单时间升序
     * （先下单先做，加菜自然落到后面）。每条记录自带所属订单信息
     * （订单号、桌号、类型、备注、手机号）。
     * <p>
     * 已被其他人认领制作的菜品不会出现在当前用户的列表中。
     */
    @SaCheckLogin
    @GetMapping("/kitchen/queue")
    public ResponseDetails kitchenQueue(@RequestParam @NotNull(message = "门店ID不能为空") Long storeId) {
        storeAuthHelper.checkStoreAnyPermission(storeId, "kitchen:manage", "order:manage", "order:view");
        Long userId = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> items = orderService.kitchenQueue(storeId, userId);
        // 菜品维度：items 即队列本体，itemCount 为菜品总数，maxWaitedMinutes 为最长等待
        result.put("items", items);
        result.put("itemCount", items.size());
        result.put("maxWaitedMinutes", items.stream()
                .mapToLong(g -> ((Number) g.get("waitedMinutes")).longValue())
                .max().orElse(0L));
        return ResponseDetails.ok().put("data", result);
    }

    /**
     * 退菜（移除未出餐的菜品）
     * <p>
     * 店员可退掉尚未出餐的菜品，恢复库存并重算订单金额。
     */
    @SaCheckLogin
    @PostMapping("/remove-item/{itemId}")
    public ResponseDetails removeItem(@PathVariable Long itemId,
                                      @RequestParam @NotNull(message = "门店ID不能为空") Long storeId,
                                      @RequestParam(value = "reason", required = false) String reason) {
        storeAuthHelper.checkStorePermission(storeId, "order:manage");
        return ResponseDetails.ok().put("data", orderService.removeOrderItem(itemId, storeId, reason));
    }

    /**
     * 调整菜品数量/重量（称重菜按实际重量重新计价）
     * <p>
     * 仅订单未结算时可调整；调整后按成交单价 × 新数量重算小计，并联动重算订单总额。
     * 支持小数（最多 3 位），如鱼按斤称重可填 1.6。
     */
    @SaCheckLogin
    @PostMapping("/adjust-item/{itemId}")
    public ResponseDetails adjustItem(@PathVariable Long itemId,
                                      @RequestParam @NotNull(message = "门店ID不能为空") Long storeId,
                                      @RequestParam @NotNull(message = "数量不能为空")
                                      @DecimalMin(value = "0.001", message = "数量必须大于 0") BigDecimal quantity) {
        storeAuthHelper.checkStorePermission(storeId, "order:manage");
        return ResponseDetails.ok().put("data", orderService.adjustOrderItemQuantity(itemId, storeId, quantity));
    }

    /**
     * 门店经营统计（数据看板）
     */
    @SaCheckLogin
    @GetMapping("/stats")
    public ResponseDetails stats(@RequestParam @NotNull(message = "门店ID不能为空") Long storeId) {
        storeAuthHelper.checkStorePermission(storeId, "order:view");
        return ResponseDetails.ok().put("data", orderService.storeStats(storeId));
    }
}
