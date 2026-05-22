package com.buguagaoshu.food.service;

import com.buguagaoshu.food.cache.RolePermissionCache;
import com.buguagaoshu.food.data.PermissionData;
import com.buguagaoshu.food.data.RoleData;
import com.buguagaoshu.food.data.RolePermissionData;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.RolePermissionDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色-权限关联服务（RBAC-D 域隔离）
 * <p>
 * 核心隔离规则：全局角色只能绑定全局权限，门店角色只能绑定门店权限
 * 绑定时校验域一致性，拒绝跨域绑定
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-21
 */
@Service
public class RolePermissionDataService {

    private final RolePermissionDataRepository rolePermissionDataRepository;

    public RolePermissionDataService(RolePermissionDataRepository rolePermissionDataRepository) {
        this.rolePermissionDataRepository = rolePermissionDataRepository;
    }

    public List<RolePermissionData> findAll() {
        List<RolePermissionData> result = new ArrayList<>();
        rolePermissionDataRepository.findAll().forEach(result::add);
        return result;
    }

    public List<RolePermissionData> findByRoleId(Long roleId) {
        return rolePermissionDataRepository.findByRoleId(roleId);
    }

    public List<RolePermissionData> findByRoleIdIn(List<Long> roleIds) {
        return rolePermissionDataRepository.findByRoleIdIn(roleIds);
    }

    /**
     * 绑定角色与权限，校验域一致性
     * 全局角色只能绑定全局权限，门店角色只能绑定门店权限
     */
    @Transactional(rollbackFor = Exception.class)
    public RolePermissionData save(RolePermissionData rolePermissionData) {
        validateDomainConsistency(rolePermissionData.getRoleId(), rolePermissionData.getPermissionId());
        return rolePermissionDataRepository.save(rolePermissionData);
    }

    public List<RolePermissionData> saveAll(List<RolePermissionData> rolePermissionDataList) {
        List<RolePermissionData> result = new ArrayList<>();
        rolePermissionDataRepository.saveAll(rolePermissionDataList).forEach(result::add);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteByRoleIdAndPermissionId(Long roleId, Long permissionId) {
        rolePermissionDataRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
        refreshRolePermissionsCache(roleId);
    }

    private void validateDomainConsistency(Long roleId, Long permissionId) {
        RoleData role = RolePermissionCache.ROLE_MAP.get(roleId);
        PermissionData perm = RolePermissionCache.PERMISSION_MAP.get(permissionId);

        if (role == null || perm == null) {
            return;
        }

        if (!role.getDomain().equals(perm.getDomain())) {
            throw new BusinessException(400, "域隔离校验失败："
                    + role.getDomain() + "域角色不能绑定" + perm.getDomain() + "域权限");
        }
    }

    private void refreshRolePermissionsCache(Long roleId) {
        List<String> permCodes = new ArrayList<>();
        rolePermissionDataRepository.findByRoleId(roleId).forEach(rp -> {
            var perm = RolePermissionCache.PERMISSION_MAP.get(rp.getPermissionId());
            if (perm != null && perm.getStatus() == 0) {
                permCodes.add(perm.getPermissionCode());
            }
        });
        RolePermissionCache.ROLE_PERMISSIONS_MAP.put(roleId, permCodes);
    }
}
