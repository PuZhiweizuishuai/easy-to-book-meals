package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.TableReservation;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TableReservationRepository extends
        PagingAndSortingRepository<TableReservation, Long>,
        CrudRepository<TableReservation, Long> {
}
