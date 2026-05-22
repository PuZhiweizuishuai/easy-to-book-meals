package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.RolePermissionData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionDataRepository extends CrudRepository<RolePermissionData, Long> {

    List<RolePermissionData> findByRoleId(Long roleId);

    List<RolePermissionData> findByPermissionId(Long permissionId);

    List<RolePermissionData> findByRoleIdIn(List<Long> roleIds);

    void deleteByRoleIdAndPermissionId(Long roleId, Long permissionId);
}
