package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSkuRepository
        extends PagingAndSortingRepository<ProductSku, Long>, CrudRepository<ProductSku, Long> {
    List<ProductSku> findByDishId(Long dishId);
}
