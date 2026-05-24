package com.buguagaoshu.food.service;

import com.buguagaoshu.food.cache.RolePermissionCache;
import com.buguagaoshu.food.data.RoleData;
import com.buguagaoshu.food.enums.RoleDomainEnum;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.RoleDataRepository;
import com.buguagaoshu.food.repository.RolePermissionDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 角色服务（RBAC-D 域感知）
 * <p>
 * 提供角色的增删改查功能，并维护角色权限缓存
 * 通过 domain 字段区分全局角色和门店角色，确保域隔离
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
        return new ArrayList<>(RolePermissionCache.ROLE_MAP.values());
    }

    public List<RoleData> findByDomain(String domain) {
        return RolePermissionCache.getRolesByDomain(domain);
    }

    public List<RoleData> findGlobalRoles() {
        return RolePermissionCache.getGlobalRoles();
    }

    public List<RoleData> findStoreTemplateRoles() {
        return RolePermissionCache.ROLE_MAP.values().stream()
                .filter(r -> RoleDomainEnum.STORE.getCode().equals(r.getDomain()) && r.getStoreId() == null)
                .collect(Collectors.toList());
    }

    public List<RoleData> findStoreCustomRoles(Long storeId) {
        return RolePermissionCache.ROLE_MAP.values().stream()
                .filter(r -> RoleDomainEnum.STORE.getCode().equals(r.getDomain()) && storeId.equals(r.getStoreId()))
                .collect(Collectors.toList());
    }

    public Optional<RoleData> findById(Long id) {
        return Optional.ofNullable(RolePermissionCache.ROLE_MAP.get(id));
    }

    public Optional<RoleData> findByRoleCode(String roleCode) {
        return Optional.ofNullable(RolePermissionCache.ROLE_CODE_MAP.get(roleCode));
    }

    @Transactional(rollbackFor = Exception.class)
    public RoleData save(RoleData roleData) {
        validateDomain(roleData);
        RoleData saved = roleDataRepository.save(roleData);
        refreshCache(saved);
        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public RoleData update(RoleData updateData) {
        RoleData existing = findById(updateData.getId())
                .orElseThrow(() -> new BusinessException(404, "角色不存在"));

        validateDomain(updateData);

        String oldRoleCode = existing.getRoleCode();

        existing.setRoleName(updateData.getRoleName());
        existing.setDescription(updateData.getDescription());
        existing.setSort(updateData.getSort());
        existing.setStatus(updateData.getStatus());

        RoleData saved = roleDataRepository.save(existing);

        if (!oldRoleCode.equals(saved.getRoleCode())) {
            RolePermissionCache.ROLE_CODE_MAP.remove(oldRoleCode);
        }
        refreshCache(saved);
        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        RoleData role = findById(id)
                .orElseThrow(() -> new BusinessException(404, "角色不存在"));

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

    private void validateDomain(RoleData roleData) {
        if (roleData.getDomain() == null) {
            throw new BusinessException(400, "角色域(domain)不能为空");
        }
        if (!RoleDomainEnum.GLOBAL.getCode().equals(roleData.getDomain())
                && !RoleDomainEnum.STORE.getCode().equals(roleData.getDomain())) {
            throw new BusinessException(400, "无效的角色域: " + roleData.getDomain());
        }
    }

    private void refreshCache(RoleData saved) {
        RolePermissionCache.ROLE_MAP.put(saved.getId(), saved);
        RolePermissionCache.ROLE_CODE_MAP.put(saved.getRoleCode(), saved);
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
