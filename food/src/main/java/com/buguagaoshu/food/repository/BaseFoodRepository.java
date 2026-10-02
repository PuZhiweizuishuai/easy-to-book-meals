package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.BaseFood;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BaseFoodRepository
        extends JpaRepository<BaseFood, Long> {
    Page<BaseFood> findByNameLike(String name, Pageable pageable);

    /**
     * 按门店查询菜品（分页，带名称模糊搜索）
     */
    Page<BaseFood> findByStorefrontIdAndNameLike(Long storefrontId, String name, Pageable pageable);

    /**
     * 按门店查询菜品（分页）
     */
    Page<BaseFood> findByStorefrontId(Long storefrontId, Pageable pageable);

    /**
     * 按门店查询所有菜品（不分页）
     */
    List<BaseFood> findByStorefrontId(Long storefrontId);

    /**
     * 按门店查询指定上架状态的菜品（顾客端菜单）
     */
    List<BaseFood> findByStorefrontIdAndStatus(Long storefrontId, Integer status);

    /**
     * 原子累加菜品销量（月销量与总销量）
     */
    @Modifying
    @Query("UPDATE BaseFood f SET f.allSales = COALESCE(f.allSales, 0) + :quantity, " +
            "f.monthlySales = COALESCE(f.monthlySales, 0) + :quantity WHERE f.id = :id")
    int addSales(@Param("id") Long id, @Param("quantity") Long quantity);
}
