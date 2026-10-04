package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 优惠券模板仓储
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-04
 */
@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    List<Coupon> findByStoreId(Long storeId);

    /**
     * 门店券模板分页查询（支持按券名称模糊搜索）
     */
    @Query("SELECT c FROM Coupon c WHERE c.storeId = :storeId " +
            "AND (:key IS NULL OR c.name LIKE %:key%) " +
            "ORDER BY c.createTime DESC")
    Page<Coupon> findByStore(@Param("storeId") Long storeId, @Param("key") String key, Pageable pageable);

    /**
     * 查询门店内当前有效（启用且在有效期内）的券模板，供结算时推荐可用券
     */
    @Query("SELECT c FROM Coupon c WHERE c.storeId = :storeId AND c.status = 0 " +
            "AND (c.startTime IS NULL OR c.startTime <= :now) " +
            "AND (c.endTime IS NULL OR c.endTime = 0 OR c.endTime >= :now) " +
            "ORDER BY c.createTime DESC")
    List<Coupon> findValidCoupons(@Param("storeId") Long storeId, @Param("now") Long now);
}
