package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductSkuRepository
        extends JpaRepository<ProductSku, Long> {
    List<ProductSku> findByDishId(Long dishId);

    List<ProductSku> findByDishIdIn(Collection<Long> dishIds);

    /**
     * 原子扣减库存（乐观策略：通过 WHERE 条件保证并发安全）
     * stock = -1 表示不限库存，直接成功且不修改库存值（保持 -1）
     * 返回受影响行数：0 表示库存不足或 SKU 已禁用
     */
    @Modifying
    @Query("UPDATE ProductSku s SET s.stock = CASE WHEN s.stock = -1 THEN -1 ELSE s.stock - :quantity END " +
            "WHERE s.id = :id AND s.status = 1 AND (s.stock = -1 OR s.stock >= :quantity)")
    int deductStock(@Param("id") Long id, @Param("quantity") Long quantity);

    /**
     * 回补库存（订单取消时调用），不限库存(-1)保持不变
     */
    @Modifying
    @Query("UPDATE ProductSku s SET s.stock = CASE WHEN s.stock = -1 THEN -1 ELSE s.stock + :quantity END " +
            "WHERE s.id = :id")
    int restoreStock(@Param("id") Long id, @Param("quantity") Long quantity);

    /**
     * 原子累加 SKU 销量
     */
    @Modifying
    @Query("UPDATE ProductSku s SET s.sales = COALESCE(s.sales, 0) + :quantity WHERE s.id = :id")
    int addSales(@Param("id") Long id, @Param("quantity") Long quantity);
}
