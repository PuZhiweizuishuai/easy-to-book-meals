package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.ReviewData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-02
 */
@Repository
public interface ReviewDataRepository extends JpaRepository<ReviewData, Long> {

    Optional<ReviewData> findByOrderId(Long orderId);

    /**
     * 分页查询门店正常展示的评价（顾客端门店页）
     */
    Page<ReviewData> findByStoreIdAndStatusOrderByCreateTimeDesc(Long storeId, Integer status, Pageable pageable);
}
