package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 角色-权限关联实体
 * 一个角色可以拥有多个权限，一个权限可以分配给多个角色
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "role_permission_table", indexes = {
        @Index(name = "idx_role_permission_role_id", columnList = "roleId"),
        @Index(name = "idx_role_permission_permission_id", columnList = "permissionId")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_role_permission", columnNames = {"roleId", "permissionId"})
})
public class RolePermissionData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * 角色ID，关联 role_table
     */
    @Column(nullable = false)
    private Long roleId;

    /**
     * 权限ID，关联 permission_table
     */
    @Column(nullable = false)
    private Long permissionId;
}
