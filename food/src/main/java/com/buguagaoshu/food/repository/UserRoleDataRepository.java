package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.UserRoleData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleDataRepository extends CrudRepository<UserRoleData, Long> {

    List<UserRoleData> findByUserId(Long userId);

    List<UserRoleData> findByRoleId(Long roleId);

    List<UserRoleData> findByStoreId(Long storeId);

    List<UserRoleData> findByStoreIdAndUserId(Long storeId, Long userId);

    List<UserRoleData> findByUserIdAndStoreIdIsNull(Long userId);

    List<UserRoleData> findByUserIdAndStoreIdIsNotNull(Long userId);

    boolean existsByUserIdAndRoleIdAndStoreId(Long userId, Long roleId, Long storeId);

    void deleteByUserIdAndRoleIdAndStoreId(Long userId, Long roleId, Long storeId);
}
