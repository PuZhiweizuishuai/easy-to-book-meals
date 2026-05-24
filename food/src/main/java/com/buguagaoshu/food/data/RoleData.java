package com.buguagaoshu.food.data;

import com.buguagaoshu.food.enums.RoleDomainEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 角色实体（RBAC-D 带数据域权限模型）
 * <p>
 * 通过 domain 字段实现全局角色与门店角色的隔离：
 * - GLOBAL 域：平台级角色，如超级管理员、平台运营、平台审核
 * - STORE 域：门店级角色，如店铺老板、收银员、后厨、服务员
 * <p>
 * 核心隔离规则：
 * 1. 全局角色只能绑定全局权限，门店角色只能绑定门店权限
 * 2. 全局角色由超管分配，门店角色由本店店主分配
 * 3. 两类角色不能交叉绑定、不能跨域授权
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "role_table", indexes = {
        @Index(name = "idx_role_table_code", columnList = "roleCode", unique = true),
        @Index(name = "idx_role_table_domain", columnList = "domain")
})
public class RoleData {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String roleCode;

    @Column(nullable = false, length = 50)
    private String roleName;

    @Column(length = 200)
    private String description;

    /**
     * 权限域: GLOBAL-全局域(平台级角色), STORE-门店域(门店级角色)
     * 全局角色只能绑定全局权限，门店角色只能绑定门店权限
     */
    @Column(nullable = false, length = 20)
    private String domain;

    /**
     * 门店ID，仅门店自定义角色有值，预设门店角色为 null
     * 预设门店角色（如 STORE_OWNER）是跨门店共享的角色模板，
     * 具体属于哪个门店由 UserRoleData.storeId 决定
     */
    private Long storeId;

    private Integer sort = 0;

    private Integer status = 0;

    /**
     * 角色关联的权限列表（非持久化，从缓存中填充）
     */
    @Transient
    private List<PermissionData> permissionList;

    public boolean isGlobal() {
        return RoleDomainEnum.GLOBAL.getCode().equals(this.domain);
    }

    public boolean isStore() {
        return RoleDomainEnum.STORE.getCode().equals(this.domain);
    }
}
