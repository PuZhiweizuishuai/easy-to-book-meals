package com.buguagaoshu.food.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 权限实体
 * 对应 Sa-Token 中的权限概念，用于 RBAC 权限模型
 * 权限数据在系统启动时加载到缓存，运行期间极少变更
 * <p>
 * 权限编码格式建议: 模块:操作，如 user:manage、order:view
 * 对应 Sa-Token 的 StpInterface 返回的权限标识
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "permission_table", indexes = {
        @Index(name = "idx_permission_table_code", columnList = "permissionCode", unique = true)
})
public class PermissionData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * 权限编码，用于程序中判断权限，如 user:manage、order:view
     * 对应 Sa-Token 的 StpInterface 返回的权限标识
     */
    @Column(unique = true, nullable = false, length = 100)
    private String permissionCode;

    /**
     * 权限名称，用于前端展示，如 用户管理、查看订单
     */
    @Column(nullable = false, length = 50)
    private String permissionName;

    /**
     * 所属模块，如 user、order、store、dish
     * 用于前端按模块分组展示权限
     */
    @Column(length = 50)
    private String module;

    /**
     * 权限描述
     */
    @Column(length = 200)
    private String description;

    /**
     * 排序值，越小越靠前
     */
    @Column(columnDefinition = "INT default 0")
    private Integer sort;

    /**
     * 状态 0-启用 1-禁用
     */
    private Integer status = 0;
}
