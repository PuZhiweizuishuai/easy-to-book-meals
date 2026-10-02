package com.buguagaoshu.food.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.buguagaoshu.food.data.OrderData;
import com.buguagaoshu.food.dto.AddOrderItemsDto;
import com.buguagaoshu.food.dto.CreateOrderDto;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器（顾客端）
 * <p>
 * 提供顾客下单、取消订单、查询我的订单等面向消费者的功能
 * 店主端订单管理见 OrderManageController
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-02
 */
@RestController
@RequestMapping("/api/order")
@Validated
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 创建订单（登录用户自主下单）
     */
    @SaCheckLogin
    @PostMapping("/create")
    public ResponseDetails createOrder(@Valid @RequestBody CreateOrderDto dto) {
        OrderData order = orderService.createOrder(dto);
        return ResponseDetails.ok().put("data", order);
    }

    /**
     * 餐桌二维码免登录下单
     * <p>
     * 扫码顾客无需注册/登录即可下单，订单类型固定为堂食并自动绑定餐桌。
     * 下单成功后餐桌状态自动置为「已占用」。
     */
    @PostMapping("/guest-create")
    public ResponseDetails guestCreateOrder(@Valid @RequestBody CreateOrderDto dto) {
        OrderData order = orderService.createGuestOrder(dto);
        return ResponseDetails.ok().put("data", order);
    }

    /**
     * 扫码免登录查询本桌当前未结算订单
     * <p>
     * 刷新页面后展示已下单菜品：通过餐桌二维码定位餐桌 → 返回其当前订单详情。
     * 订单已结算或取消时返回空 data。
     */
    @GetMapping("/guest-current")
    public ResponseDetails guestCurrentOrder(@RequestParam("qrCodeKey") String qrCodeKey) {
        return ResponseDetails.ok().put("data", orderService.getGuestCurrentOrder(qrCodeKey));
    }

    /**
     * 扫码免登录加菜（为本桌未结算订单追加菜品）
     * <p>
     * 通过 qrCodeKey 校验餐桌归属后放行，避免未登录身份下越权操作他人订单。
     */
    @PostMapping("/guest-add-items/{id}")
    public ResponseDetails guestAddItems(@PathVariable Long id,
                                        @RequestParam("qrCodeKey") String qrCodeKey,
                                        @Valid @RequestBody AddOrderItemsDto dto) {
        return ResponseDetails.ok().put("data",
                orderService.addItemsToGuestOrder(id, qrCodeKey, dto));
    }

    /**
     * 顾客取消订单（仅待接单状态可取消）
     */
    @SaCheckLogin
    @PostMapping("/cancel/{id}")
    public ResponseDetails cancelOrder(@PathVariable Long id) {
        return ResponseDetails.ok().put("data", orderService.cancelMyOrder(id));
    }

    /**
     * 顾客加菜（未结算前追加菜品，仅本人订单）
     */
    @SaCheckLogin
    @PostMapping("/add-items/{id}")
    public ResponseDetails addItems(@PathVariable Long id,
                                    @Valid @RequestBody AddOrderItemsDto dto) {
        return ResponseDetails.ok().put("data", orderService.addItemsToMyOrder(id, dto));
    }

    /**
     * 查询我的订单（分页，可按状态过滤）
     */
    @SaCheckLogin
    @GetMapping("/my")
    public ResponseDetails myOrders(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", required = false, defaultValue = "1")
            @Min(value = 1, message = "页码不能小于1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10")
            @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 100, message = "每页条数不能超过100") Integer size) {
        return ResponseDetails.ok().put("data", orderService.listMyOrders(status, page, size));
    }

    /**
     * 查询我的订单详情
     */
    @SaCheckLogin
    @GetMapping("/detail/{id}")
    public ResponseDetails myOrderDetail(@PathVariable Long id) {
        return ResponseDetails.ok().put("data", orderService.getMyOrderDetail(id));
    }
}
