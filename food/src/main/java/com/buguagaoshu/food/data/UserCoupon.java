package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 优惠券码（券实例）
 * <p>
 * 一个券码即一张券：创建券模板时按发行量批量生成，券码为 32 位无横线 UUID。
 * 券码可生成二维码供顾客展示核销，顾客登录后也可领取并关联到自己的账户。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-04
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_coupon_table", indexes = {
        @Index(name = "idx_uc_code", columnList = "code", unique = true),
        @Index(name = "idx_uc_coupon", columnList = "couponId"),
        @Index(name = "idx_uc_store_user", columnList = "storeId, userId"),
        @Index(name = "idx_uc_store_phone", columnList = "storeId, phone"),
        @Index(name = "idx_uc_status", columnList = "couponId, status")
})
@EntityListeners(AuditingEntityListener.class)
public class UserCoupon {

    /** 状态：未使用 */
    public static final int STATUS_UNUSED = 0;

    /** 状态：已使用 */
    public static final int STATUS_USED = 1;

    /** 状态：已作废（门店停用该券） */
    public static final int STATUS_INVALID = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /** 所属门店 */
    @Column(nullable = false)
    private Long storeId;

    /** 券模板ID */
    @Column(nullable = false)
    private Long couponId;

    /**
     * 券码：32 位无横线 UUID
     */
    @Column(nullable = false, unique = true, length = 64)
    private String code;

    /**
     * 关联的用户ID（未领取/未关联时为 null，此时凭券码即可核销）
     */
    private Long userId;

    /**
     * 关联手机号（领取或核销时记录，便于门店对账与会员识别）
     */
    @Column(length = 20)
    private String phone;

    /** 状态：0-未使用 1-已使用 2-已作废 */
    @Column(nullable = false)
    private Integer status = STATUS_UNUSED;

    /** 领取时间 */
    private Long claimTime;

    /** 核销时间 */
    private Long usedTime;

    /** 核销订单ID */
    private Long orderId;

    /** 核销订单号（冗余便于展示） */
    @Column(length = 64)
    private String orderNo;

    @CreatedDate
    @Column(updatable = false)
    private Long createTime;

    @LastModifiedDate
    private Long updateTime;

    /**
     * 券模板快照（非持久化，查询时填充，便于前端直接展示券规则）
     */
    @Transient
    private Coupon coupon;

    /**
     * 按当前订单金额试算出的优惠金额（非持久化，仅用于结算前端展示）
     */
    @Transient
    private java.math.BigDecimal discountAmount;

    @Transient
    public String getStatusName() {
        return switch (status == null ? STATUS_UNUSED : status) {
            case STATUS_USED -> "已使用";
            case STATUS_INVALID -> "已作废";
            default -> "未使用";
        };
    }
}
