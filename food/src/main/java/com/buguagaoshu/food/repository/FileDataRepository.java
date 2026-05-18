package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.FileData;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface FileDataRepository
        extends PagingAndSortingRepository<FileData, Long>,
        CrudRepository<FileData, Long> {

}
