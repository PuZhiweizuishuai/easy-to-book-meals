package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.FoodWithTag;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-26
 */
@Repository
public interface FoodWithTagRepository
        extends PagingAndSortingRepository<FoodWithTag, Long>,
        CrudRepository<FoodWithTag, Long> {
    List<FoodWithTag> findAllByFoodId(Long foodId);

    List<FoodWithTag> findByFoodIdIn(List<Long> foodIds);

    void deleteByFoodId(Long foodId);
}
