package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.SpecGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecGroupRepository extends JpaRepository<SpecGroup, Long> {
    List<SpecGroup> findByDishIdOrderBySortAsc(Long dishId);
}
