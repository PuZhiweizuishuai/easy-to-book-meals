package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.RoleData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleDataRepository extends CrudRepository<RoleData, Long> {

    Optional<RoleData> findByRoleCode(String roleCode);
}
