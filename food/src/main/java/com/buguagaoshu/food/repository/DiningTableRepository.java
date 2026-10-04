package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.DiningTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiningTableRepository extends
        PagingAndSortingRepository<DiningTable, Long>,
        CrudRepository<DiningTable, Long> {

    List<DiningTable> findByStorefrontIdAndDeletedOrderBySortOrderAsc(Long storefrontId, Integer deleted);

    Page<DiningTable> findByStorefrontIdAndDeletedOrderBySortOrderAsc(Long storefrontId, Integer deleted, Pageable pageable);

    Optional<DiningTable> findByIdAndDeleted(Long id, Integer deleted);

    boolean existsByStorefrontIdAndTableNumberAndDeleted(Long storefrontId, String tableNumber, Integer deleted);

    boolean existsByStorefrontIdAndTableNumberAndDeletedAndIdNot(Long storefrontId, String tableNumber, Integer deleted, Long id);

    List<DiningTable> findByStorefrontIdAndStatusAndDeletedOrderBySortOrderAsc(Long storefrontId, Integer status, Integer deleted);

    @Query("SELECT dt FROM DiningTable dt WHERE dt.storefrontId = :storefrontId AND dt.deleted = 0 AND dt.capacity >= :minCapacity ORDER BY dt.sortOrder ASC")
    List<DiningTable> findByStorefrontIdAndCapacityGreaterThanEqualAndDeleted(Long storefrontId, Integer minCapacity);

    /**
     * 通过二维码标识查询未删除的餐桌（顾客扫码点餐）
     */
    Optional<DiningTable> findByQrCodeKeyAndDeleted(String qrCodeKey, Integer deleted);

    /**
     * 查询指定状态下、更新时间早于 threshold 的餐桌（定时任务：清理中超时自动复位）
     */
    List<DiningTable> findByStatusAndUpdateTimeBefore(Integer status, Long updateTime);

    /**
     * 原子占用餐桌（下单绑定当前订单）
     * <p>
     * 条件：餐桌未删除、未停用、且当前没有绑定任何活跃订单（currentOrderId 为空）。
     * 覆盖空闲 / 已预订 / 预订到店已置占用但尚未下单 的餐桌；
     * 通过 WHERE 条件由数据库行锁保证并发安全：同一餐桌并发下单只有一条 UPDATE 成功，
     * 失败方订单整体回滚（已扣库存随之回滚），避免后下单覆盖前订单产生孤儿订单。
     *
     * @return 受影响行数：1=占用成功；0=餐桌已被其他订单抢占/已停用
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE DiningTable t SET t.currentOrderId = :orderId, t.status = :occupiedStatus, t.updateTime = :now " +
            "WHERE t.id = :id AND t.deleted = 0 AND t.status <> :disabledStatus AND t.currentOrderId IS NULL")
    int bindOrderIfFree(@Param("id") Long id,
                        @Param("orderId") Long orderId,
                        @Param("occupiedStatus") Integer occupiedStatus,
                        @Param("disabledStatus") Integer disabledStatus,
                        @Param("now") Long now);
}
