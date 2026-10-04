package com.buguagaoshu.food.data;

import com.buguagaoshu.food.enums.CouponType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;

/**
 * 优惠券模板（门店级）
 * <p>
 * 模板定义券的规则（满减/折扣/无门槛）与发行量，
 * 创建模板时按发行量批量生成 {@link UserCoupon} 券码（32 位无横线 UUID），
 * 一个券码即一张可核销的实体券，支持生成二维码供顾客展示/核销。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-04
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "coupon_table", indexes = {
        @Index(name = "idx_coupon_store", columnList = "storeId"),
        @Index(name = "idx_coupon_status", columnList = "storeId, status")
})
@EntityListeners(AuditingEntityListener.class)
public class Coupon {

    /** 状态：启用 */
    public static final int STATUS_ENABLED = 0;

    /** 状态：停用 */
    public static final int STATUS_DISABLED = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /** 所属门店 */
    @Column(nullable = false)
    private Long storeId;

    /** 券名称，如「满100减20」 */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 券类型
     * @see CouponType
     */
    @Column(nullable = false)
    private Integer type = CouponType.FULL_REDUCE.getCode();

    /**
     * 使用门槛金额（满减券/折扣券生效的最低消费；无门槛券为 0）
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal thresholdAmount = BigDecimal.ZERO;

    /**
     * 减免金额（满减券/无门槛券使用）
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal reduceAmount = BigDecimal.ZERO;

    /**
     * 折扣率（折扣券使用，0~1，如 0.85 表示 85 折）
     */
    @Column(precision = 5, scale = 3)
    private BigDecimal discountRate;

    /**
     * 折扣券最高优惠金额（为空表示不限制）
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    /** 发行总量（生成的券码数量） */
    @Column(nullable = false)
    private Integer totalCount = 0;

    /** 已被领取（关联到用户账户）的数量 */
    @Column(nullable = false)
    private Integer claimCount = 0;

    /** 已核销数量 */
    @Column(nullable = false)
    private Integer usedCount = 0;

    /**
     * 每人限领数量，0 表示不限
     */
    @Column(nullable = false)
    private Integer perLimit = 1;

    /**
     * 生效时间（毫秒时间戳）
     */
    private Long startTime;

    /**
     * 失效时间（毫秒时间戳），0 或 null 表示长期有效
     */
    private Long endTime;

    /** 状态：0-启用 1-停用 */
    @Column(nullable = false)
    private Integer status = STATUS_ENABLED;

    /** 使用说明 */
    @Column(length = 255)
    private String description;

    @CreatedDate
    @Column(updatable = false)
    private Long createTime;

    @LastModifiedDate
    private Long updateTime;

    /**
     * 券类型文案（供前端直接展示，非持久化）
     */
    @Transient
    public String getTypeName() {
        CouponType t = CouponType.fromCode(type);
        return t == null ? "优惠券" : t.getName();
    }
}
