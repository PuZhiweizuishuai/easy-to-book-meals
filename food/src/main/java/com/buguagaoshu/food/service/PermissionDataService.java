package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.PermissionData;
import com.buguagaoshu.food.enums.RoleDomainEnum;
import com.buguagaoshu.food.repository.PermissionDataRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 权限服务（RBAC-D 域感知）
 * <p>
 * 提供权限的查询功能，确保权限数据的只读性
 * 权限数据由系统初始化时写入，运行期间不允许修改
 * 通过 domain 字段区分全局权限和门店权限
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-21
 */
@Service
public class PermissionDataService {

    private final PermissionDataRepository permissionDataRepository;

    public PermissionDataService(PermissionDataRepository permissionDataRepository) {
        this.permissionDataRepository = permissionDataRepository;
    }

    public List<PermissionData> findAll() {
        List<PermissionData> result = new ArrayList<>();
        permissionDataRepository.findAll().forEach(result::add);
        return result;
    }

    public List<PermissionData> findByDomain(String domain) {
        return permissionDataRepository.findByDomain(domain);
    }

    public List<PermissionData> findGlobalPermissions() {
        return findByDomain(RoleDomainEnum.GLOBAL.getCode());
    }

    public List<PermissionData> findStorePermissions() {
        return findByDomain(RoleDomainEnum.STORE.getCode());
    }

    public Optional<PermissionData> findById(Long id) {
        return permissionDataRepository.findById(id);
    }

    public Optional<PermissionData> findByPermissionCodeAndDomain(String permissionCode, String domain) {
        return permissionDataRepository.findByPermissionCodeAndDomain(permissionCode, domain);
    }

    public List<PermissionData> saveAll(List<PermissionData> permissionDataList) {
        List<PermissionData> result = new ArrayList<>();
        permissionDataRepository.saveAll(permissionDataList).forEach(result::add);
        return result;
    }
}
