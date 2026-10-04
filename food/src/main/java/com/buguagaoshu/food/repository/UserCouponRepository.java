package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.UserCoupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 优惠券码（券实例）仓储
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-04
 */
@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    Optional<UserCoupon> findByCode(String code);

    List<UserCoupon> findByCouponId(Long couponId);

    /**
     * 券码分页查询（支持按状态过滤）
     */
    @Query("SELECT u FROM UserCoupon u WHERE u.couponId = :couponId " +
            "AND (:status IS NULL OR u.status = :status) " +
            "ORDER BY u.id DESC")
    Page<UserCoupon> findByCoupon(@Param("couponId") Long couponId,
                                  @Param("status") Integer status,
                                  Pageable pageable);

    /**
     * 查询用户在某门店持有的券（按状态过滤）
     */
    @Query("SELECT u FROM UserCoupon u WHERE u.storeId = :storeId AND u.userId = :userId " +
            "AND (:status IS NULL OR u.status = :status) " +
            "ORDER BY u.id DESC")
    List<UserCoupon> findByStoreAndUser(@Param("storeId") Long storeId,
                                        @Param("userId") Long userId,
                                        @Param("status") Integer status);

    /**
     * 查询某手机号在某门店可用的券（会员已领取或核销时登记过手机号的券）
     */
    @Query("SELECT u FROM UserCoupon u WHERE u.storeId = :storeId AND u.phone = :phone " +
            "AND u.status = 0 ORDER BY u.id DESC")
    List<UserCoupon> findAvailableByPhone(@Param("storeId") Long storeId, @Param("phone") String phone);

    /**
     * 统计某用户在某券模板下已领取/持有的券数量（每人限领校验）
     */
    long countByCouponIdAndUserId(Long couponId, Long userId);

    long countByCouponIdAndStatus(Long couponId, Integer status);

    /**
     * 统计某券模板下已被领取（关联用户）的券码数量
     */
    @Query("SELECT COUNT(u) FROM UserCoupon u WHERE u.couponId = :couponId AND u.userId IS NOT NULL")
    long countClaimedByCouponId(@Param("couponId") Long couponId);
}
