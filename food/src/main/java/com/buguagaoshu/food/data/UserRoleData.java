package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 用户-角色关联实体
 * 一个用户可以拥有多个角色，一个角色可以分配给多个用户
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
        @Index(name = "idx_user_role_role_id", columnList = "roleId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_role", columnNames = {"userId", "roleId"})
})
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
}
