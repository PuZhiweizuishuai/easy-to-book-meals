package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.PermissionData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionDataRepository extends CrudRepository<PermissionData, Long> {

    Optional<PermissionData> findByPermissionCode(String permissionCode);
}
