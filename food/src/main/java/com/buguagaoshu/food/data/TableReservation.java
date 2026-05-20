package com.buguagaoshu.food.data;

import com.buguagaoshu.food.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "table_reservation", indexes = {
        // 经常需要根据门店+状态+时间查询预定列表
        @Index(name = "idx_store_status_time", columnList = "store_id, status, reserved_start_time"),
        // 经常需要查询某张桌子在某天是否有预定
        @Index(name = "idx_table_time", columnList = "table_id, reserved_start_time")
})
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TableReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 门店ID (冗余字段，方便按门店统计和查询，避免连表)
     */
    @Column(nullable = false)
    private Long storeId;

    /**
     *
     * 确认这个订单的操作员
     * */
    private Long operator;

    /**
     * 餐桌ID
     * 注意：这里不使用 @ManyToOne 关联 DiningTable 实体，只存 ID。
     * 因为预定记录是历史数据，即使餐桌被删除，预定记录也不应受影响。
     */
    @Column(nullable = false)
    private Long tableId;

    /**
     * 预订人ID (如果是在线用户预订，关联用户表ID；如果是散客电话预订，可为空)
     */
    private Long userId;

    /**
     * 预订人姓名
     */
    @Column(nullable = false, length = 30)
    private String contactName;

    /**
     * 联系电话
     */
    @Column(nullable = false, length = 20)
    private String contactPhone;

    /**
     * 预计就餐人数
     */
    @Column(nullable = false)
    private Integer guestCount;

    /**
     * 预定状态
     */
    @Column(nullable = false, length = 20)
    private Integer status = ReservationStatus.PENDING.getCode();

    /**
     * 预定开始时间 (13位毫秒级时间戳)
     */
    @Column(nullable = false)
    private Long reservedStartTime;

    /**
     * 预定结束时间 (13位毫秒级时间戳)
     * 例如：预定中午12:00-14:00的位子
     */
    @Column(nullable = false)
    private Long reservedEndTime;

    /**
     * 取消原因 / 备注
     */
    @Column(length = 255)
    private String remark;

    /**
     * 创建时间 (13位毫秒级时间戳)
     */
    @CreatedDate
    @Column(updatable = false)
    private Long createTime;

    /**
     * 更新时间 (13位毫秒级时间戳)
     */
    @LastModifiedDate
    private Long updateTime;
}