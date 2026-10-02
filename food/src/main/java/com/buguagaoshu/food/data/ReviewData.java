package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 评价实体
 * <p>
 * 顾客在订单完成后可对门店进行评价，店家可回复
 * 一个订单只能评价一次（orderId 唯一约束）
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-02
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "review_table", indexes = {
        @Index(name = "idx_review_order", columnList = "orderId", unique = true),
        @Index(name = "idx_review_store_status", columnList = "storeId, status"),
        @Index(name = "idx_review_user", columnList = "userId")
})
@EntityListeners(AuditingEntityListener.class)
public class ReviewData {

    /**
     * 评价状态：正常展示
     */
    public static final int STATUS_NORMAL = 0;

    /**
     * 评价状态：隐藏（违规内容，管理员操作）
     */
    public static final int STATUS_HIDDEN = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * 关联订单ID（唯一，一单一评）
     */
    @Column(nullable = false, unique = true)
    private Long orderId;

    /**
     * 门店ID
     */
    @Column(nullable = false)
    private Long storeId;

    /**
     * 评价用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 评价用户昵称（快照）
     */
    @Column(length = 50)
    private String userNickname;

    /**
     * 评分 1-5 星
     */
    @Column(nullable = false)
    private Integer rating;

    /**
     * 评价内容
     */
    @Column(nullable = false, length = 500)
    private String content;

    /**
     * 商家回复
     */
    @Column(length = 500)
    private String merchantReply;

    /**
     * 商家回复时间
     */
    private Long replyTime;

    /**
     * 评价状态：0-正常 1-隐藏
     */
    @Column(nullable = false)
    private Integer status = STATUS_NORMAL;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Long createTime;

    @LastModifiedDate
    @Column(nullable = false)
    private Long updateTime;
}
