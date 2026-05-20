package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 */
@Repository
public interface StoreRepository
        extends PagingAndSortingRepository<Store, Long>, CrudRepository<Store, Long> {

    /**
     * 按店名模糊查询（分页）
     */
    Page<Store> findByStoreNameLike(String storeName, Pageable pageable);

    /**
     * 按店名模糊查询，排除指定状态（分页）
     * 用于用户端查询，不显示待审核的店面
     */
    Page<Store> findByStoreNameLikeAndStatusNotIn(String storeName, List<Integer> statusList, Pageable pageable);

    /**
     * 查询排除指定状态的店面（分页）
     */
    Page<Store> findByStatusNotIn(List<Integer> statusList, Pageable pageable);
}
