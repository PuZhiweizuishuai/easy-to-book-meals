package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.PermissionData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionDataRepository extends CrudRepository<PermissionData, Long> {

    Optional<PermissionData> findByPermissionCodeAndDomain(String permissionCode, String domain);

    List<PermissionData> findByDomain(String domain);
}
