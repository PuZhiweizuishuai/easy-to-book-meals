package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 站内消息通知实体
 * <p>
 * 通知分两类：
 * <ul>
 *   <li><b>需处理型</b>（如员工邀请 {@link #TYPE_STAFF_INVITE}）：接收人必须「同意」或「拒绝」，
 *       同意后才会真正产生业务效果（写入 user_role_table）。未处理前在店主的员工列表里显示「待确认」。</li>
 *   <li><b>告知型</b>：仅需已读，status 由 0(未读) 变为 1(已读)。</li>
 * </ul>
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-05
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification_table", indexes = {
        @Index(name = "idx_notification_user_status", columnList = "userId,status"),
        @Index(name = "idx_notification_store", columnList = "storeId,type,status")
})
public class NotificationData {

    /**
     * 通知类型：员工邀请（店主邀请用户加入门店，需用户确认）
     */
    public static final String TYPE_STAFF_INVITE = "STAFF_INVITE";

    /**
     * 状态：待处理 / 未读
     */
    public static final int STATUS_PENDING = 0;

    /**
     * 状态：已同意 / 已读
     */
    public static final int STATUS_ACCEPTED = 1;

    /**
     * 状态：已拒绝
     */
    public static final int STATUS_REJECTED = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * 接收人用户ID
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 通知类型，见 TYPE_* 常量
     */
    @Column(nullable = false, length = 32)
    private String type;

    /**
     * 通知标题
     */
    @Column(nullable = false, length = 100)
    private String title;

    /**
     * 通知正文
     */
    @Column(length = 500)
    private String content;

    /**
     * 状态：0-待处理/未读 1-已同意/已读 2-已拒绝
     */
    @Column(nullable = false)
    private Integer status = STATUS_PENDING;

    /**
     * 关联门店ID（员工邀请时为被邀请加入的门店）
     */
    private Long storeId;

    /**
     * 关联角色ID列表，逗号分隔（员工邀请时可一次邀请多个角色）
     */
    @Column(length = 255)
    private String roleIds;

    /**
     * 发起人用户ID（如发出邀请的店主）
     */
    private Long inviterId;

    /**
     * 发起人昵称快照，避免展示时再查一次用户表
     */
    @Column(length = 50)
    private String inviterName;

    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    private Long createTime = System.currentTimeMillis();

    /**
     * 处理时间（同意/拒绝/已读的时刻）
     */
    private Long handleTime;
}
