package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 角色实体
 * 对应 Sa-Token 中的角色概念，用于 RBAC 权限模型
 * 角色数据在系统启动时加载到缓存，运行期间极少变更
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "role_table", indexes = {
        @Index(name = "idx_role_table_code", columnList = "roleCode", unique = true)
})
public class RoleData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * 角色编码，用于程序中判断角色，如 SUPER_ADMIN、STORE_ADMIN
     * 对应 Sa-Token 的 StpInterface 返回的角色标识
     */
    @Column(unique = true, nullable = false, length = 50)
    private String roleCode;

    /**
     * 角色名称，用于前端展示，如 超级管理员、店面管理员
     */
    @Column(nullable = false, length = 50)
    private String roleName;

    /**
     * 角色描述
     */
    @Column(length = 200)
    private String description;

    /**
     * 排序值，越小越靠前
     */
    private Integer sort = 0;

    /**
     * 状态 0-启用 1-禁用
     */
    private Integer status = 0;
}
