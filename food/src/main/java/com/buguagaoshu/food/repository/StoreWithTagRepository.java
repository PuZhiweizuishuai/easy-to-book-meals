package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.StoreWithTag;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 */
@Repository
public interface StoreWithTagRepository
        extends PagingAndSortingRepository<StoreWithTag, Long>, CrudRepository<StoreWithTag, Long> {

    List<StoreWithTag> findAllByStoreId(Long storeId);

    List<StoreWithTag> findByStoreIdIn(List<Long> storeIds);

    void deleteByStoreId(Long storeId);
}
