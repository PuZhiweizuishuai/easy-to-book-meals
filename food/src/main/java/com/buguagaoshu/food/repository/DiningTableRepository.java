package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.DiningTable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiningTableRepository extends
        PagingAndSortingRepository<DiningTable, Long>,
        CrudRepository<DiningTable, Long> {
}
