package com.buguagaoshu.food.data;

import com.buguagaoshu.food.enums.OrderStatus;
import com.buguagaoshu.food.enums.OrderType;
import com.buguagaoshu.food.enums.PayStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单实体
 * <p>
 * 订单号生成规则：日期 + 门店ID + 时间相关序列，如 20261002-0001-0001
 * 价格采用快照模式：下单时将菜品名、规格、单价固化到 OrderItem，避免菜品后续修改影响历史订单
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-02
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_table", indexes = {
        @Index(name = "idx_order_no", columnList = "orderNo", unique = true),
        @Index(name = "idx_order_store_status", columnList = "storeId, status"),
        @Index(name = "idx_order_user", columnList = "userId"),
        @Index(name = "idx_order_table", columnList = "tableId"),
        @Index(name = "idx_order_create_time", columnList = "createTime")
})
@EntityListeners(AuditingEntityListener.class)
public class OrderData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * 订单号（唯一），格式：日期-门店ID-序列
     */
    @Column(nullable = false, unique = true, length = 40)
    private String orderNo;

    /**
     * 门店ID
     */
    @Column(nullable = false)
    private Long storeId;

    /**
     * 下单用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 下单用户昵称（快照，展示用）
     */
    @Column(length = 50)
    private String userNickname;

    /**
     * 餐桌ID（堂食扫码点餐时关联）
     */
    private Long tableId;

    /**
     * 餐桌号快照（餐桌删除后订单仍可展示）
     */
    @Column(length = 20)
    private String tableNumber;

    /**
     * 订单类型
     * @see OrderType
     * 默认 0（堂食），向后兼容历史订单
     */
    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer orderType = OrderType.DINE_IN.getCode();

    /**
     * 订单状态
     * @see OrderStatus
     */
    @Column(nullable = false)
    private Integer status = OrderStatus.PENDING.getCode();

    /**
     * 商品总金额（原价合计）
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    /**
     * 优惠金额
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * 应付金额 = totalAmount - discountAmount
     */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal payAmount = BigDecimal.ZERO;

    /**
     * 支付状态
     * @see PayStatus
     */
    @Column(nullable = false)
    private Integer payStatus = PayStatus.UNPAID.getCode();

    /**
     * 接单时间
     */
    private Long confirmTime;

    /**
     * 完成时间
     */
    private Long completeTime;

    /**
     * 结算联系电话（店员在结算完成时录入，用于会员积分累积与回头客识别）
     * <p>
     * 为空表示未录入（匿名堂食订单常见）；非空时同步写入会员积分表。
     */
    @Column(length = 20)
    private String contactPhone;

    /**
     * 订单备注（顾客留言，如：少辣、不要香菜）
     */
    @Column(length = 255)
    private String remark;

    /**
     * 取消原因（订单被取消时记录）
     */
    @Column(length = 255)
    private String cancelReason;

    /**
     * 创建时间（自动插入）
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Long createTime;

    /**
     * 更新时间（自动更新）
     */
    @LastModifiedDate
    @Column(nullable = false)
    private Long updateTime;

    /**
     * 订单明细列表（非持久化，查询时批量填充）
     */
    @Transient
    private List<OrderItem> items;
}
