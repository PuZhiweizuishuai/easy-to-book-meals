package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.Membership;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipRepository extends CrudRepository<Membership, Long> {

    List<Membership> findByUserId(Long userId);

    List<Membership> findByStoreId(Long storeId);

    List<Membership> findByStoreIdAndUserId(Long storeId, Long userId);

    List<Membership> findByPhone(String phone);

    List<Membership> findByStoreIdAndPhone(Long storeId, String phone);
}
