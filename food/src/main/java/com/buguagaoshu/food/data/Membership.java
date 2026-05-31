package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 会员表
 * */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "membership_table", indexes = {
        @Index(name = "idx_membership_table_storeId", columnList = "storeId"),
        @Index(name = "idx_membership_table_userId", columnList = "userId"),
        @Index(name = "idx_membership_table_phone", columnList = "phone")
})
@EntityListeners(AuditingEntityListener.class)
public class Membership {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long storeId;


    private Long userId;

    /**
     * 店铺内显示的昵称
     * */
    private String nickname;

    /**
     * 用户电话
     * */
    private String phone;

    /**
     * 会员备注
     * */
    @Column(columnDefinition = "TEXT")
    private String remarks;



    /**
     * 积分
     * */
    private Long num = 0L;

    /**
     * 用餐次数
     * */
    private Long count = 0L;


    /**
     * 成为会员的时间
     * */
    @CreatedDate
    private Long createTime;

    /**
     * 上次访问时间
     * */
    @LastModifiedDate
    private Long updateTime;
}
