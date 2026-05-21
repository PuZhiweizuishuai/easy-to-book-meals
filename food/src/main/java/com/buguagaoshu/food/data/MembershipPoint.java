package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 会员积分
 * */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "membership_point_table", indexes = {
        @Index(name = "idx_membership_point_table_storeId", columnList = "storeId"),
        @Index(name = "idx_membership_point_table_userId", columnList = "userId")
})
@EntityListeners(AuditingEntityListener.class)
public class MembershipPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Long storeId;

    private Long userId;

    /**
     * 积分
     * */
    private Long num = 0L;

    @CreatedDate
    private Long createTime;

    @LastModifiedDate
    private Long updateTime;
}
