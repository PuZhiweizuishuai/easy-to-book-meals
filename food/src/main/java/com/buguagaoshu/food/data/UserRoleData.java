package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 用户-角色关联实体
 * 一个用户可以拥有多个角色，一个角色可以分配给多个用户
 * <p>
 * storeId 为 null 时表示系统级角色（如超级管理员、顾客）
 * storeId 不为 null 时表示店面级角色（如某店的店长、店员），实现基于店面的权限隔离
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_role_table", indexes = {
        @Index(name = "idx_user_role_user_id", columnList = "userId"),
        @Index(name = "idx_user_role_role_id", columnList = "roleId"),
        @Index(name = "idx_user_role_store_id", columnList = "storeId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_role", columnNames = {"userId", "roleId", "storeId"})
})
@EntityListeners(AuditingEntityListener.class)
public class UserRoleData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * 用户ID，关联 user_table
     */
    @Column(nullable = false)
    private Long userId;

    /**
     * 角色ID，关联 role_table
     */
    @Column(nullable = false)
    private Long roleId;

    /**
     * 店面ID，关联 store_table
     * 为 null 时表示系统级角色，不为 null 时表示店面级角色
     */
    private Long storeId;

    /**
     * 权限操作人
     */
    private Long createUser;

    /**
     * 创建时间
     */
    @CreatedDate
    private Long createTime;
}
