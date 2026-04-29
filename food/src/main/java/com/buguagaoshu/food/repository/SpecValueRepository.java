package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.SpecValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecValueRepository
        extends PagingAndSortingRepository<SpecValue, Long>,
        CrudRepository<SpecValue, Long> {

    List<SpecValue> findAllByGroupIdIn(List<Long> groupIds);
}
