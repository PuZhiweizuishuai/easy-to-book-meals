package com.buguagaoshu.food.data;

import com.buguagaoshu.food.enums.StoreStatus;
import com.buguagaoshu.food.utils.IpUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;


/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 * 店面
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "store_table", indexes = {
        @Index(name = "idx_store_user", columnList = "createUser")
})
@EntityListeners(AuditingEntityListener.class)
public class Store {

    /**
     * 主键ID（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * 创始人
     * */
    private Long createUser;

    /**
     * 店面名称（唯一、非空）
     */
    @Column(nullable = false, unique = true, length = 50)
    private String storeName;

    /**
     * 店面详细地址
     */
    @Column(nullable = false, length = 200)
    private String address;

    /**
     * 联系电话
     */
    @Column(nullable = false, length = 20)
    private String phone;

    /**
     * 店面Logo图片地址
     */
    @Column(length = 255)
    private String logoUrl;

    /**
     * 营业时间（例：09:00-21:00）
     */
    @Column(length = 50)
    private String businessHours;

    /**
     * 店面简介/描述
     */
    @Column(columnDefinition = "longtext")
    private String description;

    /**
     * 店面状态（枚举：营业/休息/停业）
     */
    @Column(nullable = false, length = 20)
    private Integer status = StoreStatus.PENDING_REVIEW.getCode();

    /**
     * 排序号（后台管理排序用）
     */
    @Column(columnDefinition = "int default 0")
    private Integer sort;

    /**
     * 逻辑删除标志（0=未删除，1=已删除）
     */
    private Integer isDeleted = 0;

    /**
     * 月销量
     */
    private Long monthlySales;

    /**
     * 总销量
     */
    private Long allSales;

    /**
     * 开店申请客户端 IP（审计用）
     */
    @Column(length = IpUtils.IP_MAX_LENGTH)
    private String clientIp;

    /**
     * 开店申请客户端 User-Agent（审计用）
     */
    @Column(length = IpUtils.USER_AGENT_MAX_LENGTH)
    private String clientUa;

    /**
     * 开店申请客户端 IP 位置信息（审计用，ip2region 离线库解析）
     * <p>
     * 形如「中国|广东省|深圳市|电信」；关闭 {@code food.ip-audit.enabled} 或内网地址时为 null。
     */
    @Column(length = IpUtils.IP_LOCATION_MAX_LENGTH)
    private String clientIpLocation;


    /**
     * 创建时间（自动填充）
     */
    @CreatedDate
    private Long createTime;

    /**
     * 更新时间（自动更新）
     */
    @LastModifiedDate
    private Long updateTime;

    /**
     * 店面关联的标签列表
     * 接收前端标签数据、展示标签数据
     * 标签关联数据通过 StoreWithTag 中间表单独维护
     */
    @Transient
    private List<StoreTag> tags;

    /**
     * 当前用户是否在本店拥有门店域角色（仅“我管理的门店”查询时填充）
     * 待审核门店的创建者尚无店主角色，此标记为 false，前端据此隐藏“进入管理”入口
     */
    @Transient
    private Boolean hasManageRole;
}