package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.DiningTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
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
}
