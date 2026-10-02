package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 会员积分流水（每笔积分变动的明细记录）
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-03
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "points_transaction_table", indexes = {
        @Index(name = "idx_pt_membership", columnList = "membershipId"),
        @Index(name = "idx_pt_store_phone", columnList = "storeId,phone"),
        @Index(name = "idx_pt_order", columnList = "orderId")
})
@EntityListeners(AuditingEntityListener.class)
public class PointsTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /** 门店ID */
    @Column(nullable = false)
    private Long storeId;

    /** 关联会员记录ID（可为空，未注册用户先记录手机号） */
    private Long membershipId;

    /** 手机号（统一关联键，即使未注册也记录） */
    @Column(nullable = false, length = 20)
    private String phone;

    /** 关联订单ID（消费积分时填写，手动调整为空） */
    private Long orderId;

    /** 关联订单号（冗余便于展示） */
    @Column(length = 64)
    private String orderNo;

    /** 积分变动值（正数=获取，负数=扣减/消费） */
    @Column(nullable = false)
    private Long points;

    /** 变动类型：EARN=消费获取, REDEEM=积分消费, ADJUST=手动调整 */
    @Column(nullable = false, length = 20)
    private String type;

    /** 备注 */
    @Column(length = 255)
    private String remark;

    /** 操作人ID（手动调整时记录） */
    private Long operatorId;

    @CreatedDate
    @Column(updatable = false)
    private Long createTime;

    public static final String TYPE_EARN = "EARN";
    public static final String TYPE_REDEEM = "REDEEM";
    public static final String TYPE_ADJUST = "ADJUST";
}
