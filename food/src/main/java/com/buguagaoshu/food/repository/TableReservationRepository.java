package com.buguagaoshu.food.repository;

import com.buguagaoshu.food.data.TableReservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TableReservationRepository extends
        PagingAndSortingRepository<TableReservation, Long>,
        CrudRepository<TableReservation, Long> {

    List<TableReservation> findByStoreIdAndStatusOrderByReservedStartTimeAsc(Long storeId, Integer status);

    Page<TableReservation> findByStoreIdAndStatusOrderByReservedStartTimeAsc(Long storeId, Integer status, Pageable pageable);

    Page<TableReservation> findByStoreIdOrderByReservedStartTime(Long storeId, Pageable pageable);

    List<TableReservation> findByTableIdAndStatusOrderByReservedStartTimeAsc(Long tableId, Integer status);

    Optional<TableReservation> findByIdAndStoreId(Long id, Long storeId);

    @Query("SELECT tr FROM TableReservation tr WHERE tr.tableId = :tableId " +
            "AND tr.status IN :activeStatuses " +
            "AND tr.reservedStartTime < :endTime " +
            "AND tr.reservedEndTime > :startTime " +
            "ORDER BY tr.reservedStartTime ASC")
    List<TableReservation> findConflictingReservations(Long tableId, List<Integer> activeStatuses, Long startTime, Long endTime);

    @Query("SELECT tr FROM TableReservation tr WHERE tr.tableId = :tableId " +
            "AND tr.status IN :activeStatuses " +
            "AND tr.reservedStartTime < :endTime " +
            "AND tr.reservedEndTime > :startTime " +
            "AND tr.id <> :excludeId " +
            "ORDER BY tr.reservedStartTime ASC")
    List<TableReservation> findConflictingReservationsExcludingId(Long tableId, List<Integer> activeStatuses, Long startTime, Long endTime, Long excludeId);

    List<TableReservation> findByUserIdOrderByReservedStartTimeDesc(Long userId);

    Page<TableReservation> findByUserIdOrderByReservedStartTimeDesc(Long userId, Pageable pageable);
}
