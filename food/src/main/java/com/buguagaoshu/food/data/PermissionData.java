package com.buguagaoshu.food.data;

import com.buguagaoshu.food.enums.RoleDomainEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 权限实体（RBAC-D 带数据域权限模型）
 * <p>
 * 通过 domain 字段实现全局权限与门店权限的隔离：
 * - GLOBAL 域：平台级权限，如用户管理、店面审核、系统配置
 * - STORE 域：门店级权限，如菜品管理、订单管理、餐桌管理
 * <p>
 * 核心隔离规则：
 * 1. 全局权限只能分配给全局角色，门店权限只能分配给门店角色
 * 2. 两套权限池互不通用，不能跨域绑定
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "permission_table", uniqueConstraints = {
        @UniqueConstraint(name = "uk_permission_code_domain", columnNames = {"permissionCode", "domain"})
}, indexes = {
        @Index(name = "idx_permission_table_domain", columnList = "domain")
})
public class PermissionData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, length = 100)
    private String permissionCode;

    @Column(nullable = false, length = 50)
    private String permissionName;

    @Column(length = 50)
    private String module;

    @Column(length = 200)
    private String description;

    /**
     * 权限域: GLOBAL-全局域(平台级权限), STORE-门店域(门店级权限)
     * 全局权限只能分配给全局角色，门店权限只能分配给门店角色
     */
    @Column(nullable = false, length = 20)
    private String domain;

    @Column(columnDefinition = "INT default 0")
    private Integer sort;

    private Integer status = 0;

    public boolean isGlobal() {
        return RoleDomainEnum.GLOBAL.getCode().equals(this.domain);
    }

    public boolean isStore() {
        return RoleDomainEnum.STORE.getCode().equals(this.domain);
    }
}
