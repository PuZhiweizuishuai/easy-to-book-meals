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
     * 店员逐个菜品确认出餐，当订单所有正常菜品均出餐后自动推进订单到「已出餐」状态。
     */
    @SaCheckLogin
    @PostMapping("/serve-item/{itemId}")
    public ResponseDetails serveItem(@PathVariable Long itemId,
                                     @RequestParam @NotNull(message = "门店ID不能为空") Long storeId) {
        storeAuthHelper.checkStorePermission(storeId, "order:manage");
        return ResponseDetails.ok().put("data", orderService.serveOrderItem(itemId, storeId));
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
