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
        List<RoleData> result = new ArrayList<>();
        roleDataRepository.findAll().forEach(result::add);
        return result;
    }

    public List<RoleData> findByDomain(String domain) {
        return roleDataRepository.findByDomain(domain);
    }

    public List<RoleData> findGlobalRoles() {
        return findByDomain(RoleDomainEnum.GLOBAL.getCode());
    }

    public List<RoleData> findStoreTemplateRoles() {
        return roleDataRepository.findByDomainAndStoreIdIsNull(RoleDomainEnum.STORE.getCode());
    }

    public List<RoleData> findStoreCustomRoles(Long storeId) {
        return roleDataRepository.findByDomainAndStoreId(RoleDomainEnum.STORE.getCode(), storeId);
    }

    public Optional<RoleData> findById(Long id) {
        return roleDataRepository.findById(id);
    }

    public Optional<RoleData> findByRoleCode(String roleCode) {
        return roleDataRepository.findByRoleCode(roleCode);
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
