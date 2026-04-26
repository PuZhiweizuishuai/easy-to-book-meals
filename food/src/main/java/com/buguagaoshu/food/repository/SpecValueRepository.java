package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.SpecValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecValueRepository extends JpaRepository<SpecValue, Long> {
    List<SpecValue> findByGroupIdOrderBySortAsc(Long groupId);
}
