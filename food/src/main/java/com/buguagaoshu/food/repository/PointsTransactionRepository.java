package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.PointsTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointsTransactionRepository extends JpaRepository<PointsTransaction, Long> {

    /** 按会员记录ID分页查询积分流水 */
    Page<PointsTransaction> findByMembershipIdOrderByCreateTimeDesc(Long membershipId, Pageable pageable);

    /** 按门店+手机号分页查询积分流水 */
    Page<PointsTransaction> findByStoreIdAndPhoneOrderByCreateTimeDesc(Long storeId, String phone, Pageable pageable);

    /** 按门店+手机号查询所有流水（不分页） */
    List<PointsTransaction> findByStoreIdAndPhoneOrderByCreateTimeDesc(Long storeId, String phone);
}
