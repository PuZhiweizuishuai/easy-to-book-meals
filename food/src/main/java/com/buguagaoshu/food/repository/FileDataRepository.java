package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.FileData;
import com.buguagaoshu.food.enums.FileExternalTypeEnum;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface FileDataRepository
        extends PagingAndSortingRepository<FileData, Long>,
        CrudRepository<FileData, Long> {

    List<FileData> findAllByIdIn(List<Long> ids);



    List<FileData> findByExternalIdAndStatusAndType(
            Long externalId, Integer status, Integer type
    );
}
