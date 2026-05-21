package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.MembershipPoint;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipPointRepository extends CrudRepository<MembershipPoint, Long> {

    List<MembershipPoint> findByUserId(Long userId);

    List<MembershipPoint> findByStoreId(Long storeId);

    List<MembershipPoint> findByStoreIdAndUserId(Long storeId, Long userId);
}
