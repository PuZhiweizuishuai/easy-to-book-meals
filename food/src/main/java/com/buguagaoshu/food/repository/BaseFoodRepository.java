package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.BaseFood;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BaseFoodRepository
        extends PagingAndSortingRepository<BaseFood, Long> , CrudRepository<BaseFood, Long> {
    Page<BaseFood> findByNameLike(String name, Pageable pageable);
}
