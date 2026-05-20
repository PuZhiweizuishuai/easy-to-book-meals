package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.UserRoleData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleDataRepository extends CrudRepository<UserRoleData, Long> {

    List<UserRoleData> findByUserId(Long userId);

    List<UserRoleData> findByRoleId(Long roleId);

    void deleteByUserIdAndRoleId(Long userId, Long roleId);
}
