package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 订单明细实体（价格快照）
 * <p>
 * 下单时固化菜品名、规格名、单价，后续菜品/SKU 修改不影响历史订单展示
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-02
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_item_table", indexes = {
        @Index(name = "idx_order_item_order_id", columnList = "orderId"),
        @Index(name = "idx_order_item_dish_id", columnList = "dishId"),
        @Index(name = "idx_order_item_sku_id", columnList = "skuId")
})
public class OrderItem {

    /**
     * 明细状态：正常
     */
    public static final int STATUS_NORMAL = 0;

    /**
     * 明细状态：已取消（整单取消时标记）
     */
    public static final int STATUS_CANCELLED = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * 所属订单ID
     */
    @Column(nullable = false)
    private Long orderId;

    /**
     * 菜品ID
     */
    @Column(nullable = false)
    private Long dishId;

    /**
     * SKU ID
     */
    @Column(nullable = false)
    private Long skuId;

    /**
     * 菜品名称（快照）
     */
    @Column(nullable = false, length = 100)
    private String dishName;

    /**
     * 菜品封面图（快照，展示用）
     */
    @Column(length = 255)
    private String topImage;

    /**
     * 规格名称快照，如 "大份/微辣"
     */
    @Column(length = 200)
    private String specNames;

    /**
     * 购买数量 / 称重重量
     * <p>
     * 采用小数以同时支持「计件菜」（如 2 份）与「称重菜」（如鱼 1.6 斤）。
     * 称重菜下单时按预估数量（如 1）占位，出餐/结算前由后台按实际重量调整。
     */
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    /**
     * SKU 原始单价（快照）
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /**
     * 成交单价（应用折扣后，快照）
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal dealPrice;

    /**
     * 小计金额 = dealPrice * quantity
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    /**
     * 明细状态：0-正常 1-已取消
     */
    @Column(nullable = false)
    private Integer status = STATUS_NORMAL;

    /**
     * 是否已出餐：0-未出餐 1-已出餐
     * <p>
     * 出餐标记为菜品级，店员逐个菜品确认出餐；订单整体"已出餐"状态
     * 由系统在该订单所有正常明细均 served=1 时自动推进。
     */
    @Column(nullable = false)
    private Integer served = 0;

    /**
     * 是否为加菜追加的明细：0-初始下单 1-加菜追加
     * <p>
     * 用于后台订单展示时特别标注加菜菜品，避免店家遗漏处理。
     */
    @Column(nullable = false)
    private Integer addedLater = 0;

    /**
     * 开始制作的操作人ID（null 表示尚无人认领）
     */
    private Long preparingBy;

    /**
     * 开始制作的时间戳
     */
    private Long preparingTime;

    /**
     * 明细创建时间（用于加菜批次排序展示）
     */
    @Column(nullable = false, updatable = false)
    private Long createTime = System.currentTimeMillis();
}
