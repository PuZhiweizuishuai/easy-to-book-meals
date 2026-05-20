package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.StoreTag;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;
import java.util.Set;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-05-20
 */
@Repository
public interface StoreTagRepository
        extends PagingAndSortingRepository<StoreTag, Long>, CrudRepository<StoreTag, Long> {
    List<StoreTag> findAllByIdIn(Set<Long> ids);
}
