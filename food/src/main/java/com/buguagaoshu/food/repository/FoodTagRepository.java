package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.FoodTag;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-26
 */
@Repository
public interface FoodTagRepository
        extends PagingAndSortingRepository<FoodTag, Long>, CrudRepository<FoodTag, Long> {
}
