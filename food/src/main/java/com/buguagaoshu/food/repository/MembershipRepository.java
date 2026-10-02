package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.Membership;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {

    List<Membership> findByUserId(Long userId);

    List<Membership> findByStoreId(Long storeId);

    Page<Membership> findByStoreId(Long storeId, Pageable pageable);

    List<Membership> findByStoreIdAndUserId(Long storeId, Long userId);

    List<Membership> findByPhone(String phone);

    List<Membership> findByStoreIdAndPhone(Long storeId, String phone);

    Page<Membership> findByStoreIdAndPhoneContaining(Long storeId, String phone, Pageable pageable);

    Optional<Membership> findByIdAndStoreId(Long id, Long storeId);
}
