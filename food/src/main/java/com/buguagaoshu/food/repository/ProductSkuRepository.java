package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {
    List<ProductSku> findByDishId(Long dishId);
}
