package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.SpecGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecGroupRepository
        extends PagingAndSortingRepository<SpecGroup, Long>,
        CrudRepository<SpecGroup, Long> {
    List<SpecGroup> findByDishIdOrderBySortAsc(Long dishId);

    List<SpecGroup> findByDishId(Long dishId);
}
