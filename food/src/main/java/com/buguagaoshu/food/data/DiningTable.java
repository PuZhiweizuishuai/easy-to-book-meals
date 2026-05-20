package com.buguagaoshu.food.data;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@Table(name = "dining_table", uniqueConstraints = {
                @UniqueConstraint(name = "uk_store_table_number", columnNames = {"store_id", "table_number"}),
        },
        indexes = {
                @Index(name = "idx_dining_table_storefront_id", columnList = "storefrontId")
        }
)
@Data
@EntityListeners(AuditingEntityListener.class)
public class DiningTable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * 门店ID（如果支持多门店必填）
     * 如果目前只有单店，可以删除此字段，并修改唯一约束为只约束 table_number
     */
    @Column(name = "storefront_id", nullable = false)
    private Long storefrontId;

    /**
     * 餐桌号/桌名（如：A1, 2号桌, VIP-01）
     */
    @Column(name = "table_number", nullable = false, length = 20)
    private String tableNumber;

    /**
     * 座位数/容纳人数
     */
    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    /**
     * 餐桌当前状态
     */
    private Integer status;

    /**
     * 餐桌位置/区域（如：大厅、包间、露台）
     */
    @Column(name = "area", length = 30)
    private String area;

    /**
     * 二维码唯一标识 (用于扫码点餐)
     * 顾客扫描带此标识的二维码，后端通过此标识定位到具体门店的具体餐桌
     */
    @Column(name = "qr_code_key", unique = true, length = 64)
    private String qrCodeKey;

    /**
     * 当前占用该桌的活跃订单ID (方便快速定位当前账单)
     * 注意：也可以不存这个字段，通过 Order 表去反查。存这里可以提升查询性能，但需要做好一致性维护。
     */
    @Column(name = "current_order_id")
    private Long currentOrderId;

    /**
     * 排序权重 (数字越小越靠前，用于服务员端排序列表)
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 备注 (例如：靠窗、腿有点晃)
     */
    @Column(name = "remark", length = 255)
    private String remark;

    /**
     * 逻辑删除标志 (0-未删除 1-已删除)
     */
    @Column(name = "is_deleted")
    private Integer deleted = 0;

    @CreatedDate
    @Column(name = "create_time", updatable = false)
    private Long createTime;

    @LastModifiedDate
    @Column(name = "update_time")
    private Long updateTime;
}