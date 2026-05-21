package com.buguagaoshu.food.service;

import com.buguagaoshu.food.cache.RolePermissionCache;
import com.buguagaoshu.food.data.RoleData;
import com.buguagaoshu.food.repository.RoleDataRepository;
import com.buguagaoshu.food.repository.RolePermissionDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 角色服务
 * 提供角色的增删改查功能，并维护角色权限缓存
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-21
 */
@Service
public class RoleDataService {

    private final RoleDataRepository roleDataRepository;
    private final RolePermissionDataRepository rolePermissionDataRepository;

    public RoleDataService(RoleDataRepository roleDataRepository,
                           RolePermissionDataRepository rolePermissionDataRepository) {
        this.roleDataRepository = roleDataRepository;
        this.rolePermissionDataRepository = rolePermissionDataRepository;
    }

    public List<RoleData> findAll() {
        List<RoleData> result = new ArrayList<>();
        roleDataRepository.findAll().forEach(result::add);
        return result;
    }

    public Optional<RoleData> findById(Long id) {
        return roleDataRepository.findById(id);
    }

    public Optional<RoleData> findByRoleCode(String roleCode) {
        return roleDataRepository.findByRoleCode(roleCode);
    }

    @Transactional(rollbackFor = Exception.class)
    public RoleData save(RoleData roleData) {
        RoleData saved = roleDataRepository.save(roleData);
        RolePermissionCache.ROLE_MAP.put(saved.getId(), saved);
        RolePermissionCache.ROLE_CODE_MAP.put(saved.getRoleCode(), saved);
        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public RoleData update(RoleData updateData) {
        RoleData existing = findById(updateData.getId())
                .orElseThrow(() -> new com.buguagaoshu.food.exception.BusinessException(404, "角色不存在"));

        String oldRoleCode = existing.getRoleCode();

        existing.setRoleName(updateData.getRoleName());
        existing.setDescription(updateData.getDescription());
        existing.setSort(updateData.getSort());
        existing.setStatus(updateData.getStatus());

        RoleData saved = roleDataRepository.save(existing);

        if (!oldRoleCode.equals(updateData.getRoleCode())) {
            RolePermissionCache.ROLE_CODE_MAP.remove(oldRoleCode);
        }
        RolePermissionCache.ROLE_MAP.put(saved.getId(), saved);
        RolePermissionCache.ROLE_CODE_MAP.put(saved.getRoleCode(), saved);

        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        RoleData role = findById(id)
                .orElseThrow(() -> new com.buguagaoshu.food.exception.BusinessException(404, "角色不存在"));

        rolePermissionDataRepository.findByRoleId(id).forEach(rp -> {
            rolePermissionDataRepository.delete(rp);
            refreshRolePermissionsCache(rp.getRoleId());
        });

        roleDataRepository.deleteById(id);

        RolePermissionCache.ROLE_MAP.remove(id);
        RolePermissionCache.ROLE_CODE_MAP.remove(role.getRoleCode());
        RolePermissionCache.ROLE_PERMISSIONS_MAP.remove(id);
    }

    public List<RoleData> saveAll(List<RoleData> roleDataList) {
        List<RoleData> result = new ArrayList<>();
        roleDataRepository.saveAll(roleDataList).forEach(result::add);
        return result;
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
