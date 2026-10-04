package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.OrderData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-02
 */
@Repository
public interface OrderDataRepository extends JpaRepository<OrderData, Long> {

    Optional<OrderData> findByOrderNo(String orderNo);

    /**
     * 店主端分页查询门店订单（可选状态过滤、订单类型过滤、订单号模糊搜索）
     */
    @Query("SELECT o FROM OrderData o WHERE o.storeId = :storeId " +
            "AND (:status IS NULL OR o.status = :status) " +
            "AND (:orderType IS NULL OR o.orderType = :orderType) " +
            "AND (:key IS NULL OR o.orderNo LIKE %:key%) " +
            "ORDER BY o.createTime DESC")
    Page<OrderData> findStoreOrders(@Param("storeId") Long storeId,
                                    @Param("status") Integer status,
                                    @Param("orderType") Integer orderType,
                                    @Param("key") String key,
                                    Pageable pageable);

    /**
     * 顾客端分页查询自己的订单（可选状态过滤）
     */
    @Query("SELECT o FROM OrderData o WHERE o.userId = :userId " +
            "AND (:status IS NULL OR o.status = :status) " +
            "ORDER BY o.createTime DESC")
    Page<OrderData> findUserOrders(@Param("userId") Long userId,
                                   @Param("status") Integer status,
                                   Pageable pageable);

    /**
     * 查询超时未接单的订单（定时任务自动取消）
     */
    List<OrderData> findByStatusAndCreateTimeBefore(Integer status, Long createTime);

    /**
     * 取消订单的原子状态翻转（CAS 幂等保护）
     * <p>
     * 仅当订单当前状态属于可取消状态（待接单/已接单）时才翻转为已取消。
     * 顾客取消、店家拒单、超时定时任务并发执行时，只有一条 UPDATE 成功，
     * 成功者才继续恢复库存，从根本上杜绝库存被重复恢复两倍的问题。
     * 批量 UPDATE 不触发 JPA 审计，updateTime 需手动写入。
     *
     * @return 受影响行数：1=成功抢占取消；0=订单已被取消或状态已推进
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE OrderData o SET o.status = :cancelledStatus, o.cancelReason = :reason, o.updateTime = :now " +
            "WHERE o.id = :id AND o.status IN :cancellableStatuses")
    int cancelIfCancellable(@Param("id") Long id,
                            @Param("cancelledStatus") Integer cancelledStatus,
                            @Param("reason") String reason,
                            @Param("now") Long now,
                            @Param("cancellableStatuses") java.util.Collection<Integer> cancellableStatuses);

    /**
     * 统计门店某时间之后的订单量
     */
    long countByStoreIdAndCreateTimeGreaterThanEqual(Long storeId, Long createTime);

    /**
     * 统计门店某状态订单量
     */
    long countByStoreIdAndStatus(Long storeId, Integer status);

    /**
     * 统计门店某时间之后的营业额（已完成订单）
     */
    @Query("SELECT COALESCE(SUM(o.payAmount), 0) FROM OrderData o " +
            "WHERE o.storeId = :storeId AND o.status = 4 AND o.createTime >= :startTime")
    BigDecimal sumStoreRevenue(@Param("storeId") Long storeId, @Param("startTime") Long startTime);

    /**
     * 查询餐桌当前关联的活跃订单
     */
    Optional<OrderData> findFirstByTableIdAndStatusInOrderByCreateTimeDesc(Long tableId, List<Integer> statuses);

    /**
     * 按门店+联系电话分页查询订单（会员消费记录）
     */
    @Query("SELECT o FROM OrderData o WHERE o.storeId = :storeId AND o.contactPhone = :phone " +
            "ORDER BY o.createTime DESC")
    Page<OrderData> findStoreOrdersByPhone(@Param("storeId") Long storeId,
                                          @Param("phone") String phone,
                                          Pageable pageable);

    /**
     * 统计门店某手机号的消费记录数量（会员用餐次数）
     */
    long countByStoreIdAndContactPhone(Long storeId, String contactPhone);
}
