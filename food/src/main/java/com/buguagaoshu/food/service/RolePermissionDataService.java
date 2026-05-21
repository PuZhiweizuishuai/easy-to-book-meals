package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.RolePermissionData;
import com.buguagaoshu.food.repository.RolePermissionDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色-权限关联服务
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

    @Transactional(rollbackFor = Exception.class)
    public RolePermissionData save(RolePermissionData rolePermissionData) {
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
    }
}
