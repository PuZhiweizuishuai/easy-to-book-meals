package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.DiningTable;
import com.buguagaoshu.food.data.TableReservation;
import com.buguagaoshu.food.dto.CreateReservationDto;
import com.buguagaoshu.food.dto.UpdateReservationDto;
import com.buguagaoshu.food.enums.ReservationStatus;
import com.buguagaoshu.food.enums.ReturnCodeEnum;
import com.buguagaoshu.food.enums.TableStatus;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.TableReservationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class TableReservationService {
    private final TableReservationRepository tableReservationRepository;
    private final DiningTableService diningTableService;

    private static final List<Integer> ACTIVE_STATUSES = Arrays.asList(
            ReservationStatus.PENDING.getCode(),
            ReservationStatus.CONFIRMED.getCode()
    );

    public TableReservationService(TableReservationRepository tableReservationRepository,
                                   DiningTableService diningTableService) {
        this.tableReservationRepository = tableReservationRepository;
        this.diningTableService = diningTableService;
    }

    @Transactional(rollbackFor = Exception.class)
    public TableReservation createReservation(CreateReservationDto dto) {
        if (dto.getReservedStartTime() >= dto.getReservedEndTime()) {
            throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(), "预定开始时间必须早于结束时间");
        }

        if (dto.getReservedStartTime() < System.currentTimeMillis()) {
            throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(), "预定开始时间不能早于当前时间");
        }

        DiningTable table = diningTableService.getTable(dto.getTableId(), dto.getStoreId());

        if (table.getCapacity() < dto.getGuestCount()) {
            throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(),
                    "餐桌座位数(" + table.getCapacity() + ")不足以容纳" + dto.getGuestCount() + "人");
        }

        List<TableReservation> conflicts = tableReservationRepository.findConflictingReservations(
                dto.getTableId(), ACTIVE_STATUSES, dto.getReservedStartTime(), dto.getReservedEndTime());
        if (!conflicts.isEmpty()) {
            throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(), "该时段餐桌已被预定，存在时间冲突");
        }

        TableReservation reservation = new TableReservation();
        reservation.setStoreId(dto.getStoreId());
        reservation.setTableId(dto.getTableId());
        reservation.setUserId(dto.getUserId());
        reservation.setContactName(dto.getContactName());
        reservation.setContactPhone(dto.getContactPhone());
        reservation.setGuestCount(dto.getGuestCount());
        reservation.setStatus(ReservationStatus.PENDING.getCode());
        reservation.setReservedStartTime(dto.getReservedStartTime());
        reservation.setReservedEndTime(dto.getReservedEndTime());
        reservation.setRemark(dto.getRemark());
        reservation.setOperator(dto.getOperator());

        TableReservation saved = tableReservationRepository.save(reservation);

        if (table.getStatus() == TableStatus.FREE.getCode()) {
            diningTableService.updateTableStatus(dto.getTableId(), dto.getStoreId(), TableStatus.RESERVED.getCode());
        }

        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public TableReservation updateReservation(UpdateReservationDto dto) {
        TableReservation reservation = findReservationOrThrow(dto.getId(), dto.getStoreId());

        if (reservation.getStatus().equals(ReservationStatus.CANCELLED.getCode()) ||
                reservation.getStatus().equals(ReservationStatus.COMPLETED.getCode())) {
            throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(), "已取消或已完成的预定不能修改");
        }

        boolean timeChanged = false;
        Long newStartTime = dto.getReservedStartTime() != null ? dto.getReservedStartTime() : reservation.getReservedStartTime();
        Long newEndTime = dto.getReservedEndTime() != null ? dto.getReservedEndTime() : reservation.getReservedEndTime();

        if (dto.getReservedStartTime() != null || dto.getReservedEndTime() != null) {
            if (newStartTime >= newEndTime) {
                throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(), "预定开始时间必须早于结束时间");
            }
            timeChanged = true;
        }

        if (dto.getTableId() != null && !dto.getTableId().equals(reservation.getTableId())) {
            DiningTable newTable = diningTableService.getTable(dto.getTableId(), dto.getStoreId());
            if (dto.getGuestCount() != null && newTable.getCapacity() < dto.getGuestCount()) {
                throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(),
                        "新餐桌座位数不足以容纳预定人数");
            }
            timeChanged = true;
        }

        if (timeChanged) {
            Long checkTableId = dto.getTableId() != null ? dto.getTableId() : reservation.getTableId();
            List<TableReservation> conflicts = tableReservationRepository.findConflictingReservationsExcludingId(
                    checkTableId, ACTIVE_STATUSES, newStartTime, newEndTime, dto.getId());
            if (!conflicts.isEmpty()) {
                throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(), "修改后的时间与已有预定冲突");
            }
        }

        if (dto.getTableId() != null) {
            reservation.setTableId(dto.getTableId());
        }
        if (dto.getContactName() != null) {
            reservation.setContactName(dto.getContactName());
        }
        if (dto.getContactPhone() != null) {
            reservation.setContactPhone(dto.getContactPhone());
        }
        if (dto.getGuestCount() != null) {
            reservation.setGuestCount(dto.getGuestCount());
        }
        if (dto.getReservedStartTime() != null) {
            reservation.setReservedStartTime(dto.getReservedStartTime());
        }
        if (dto.getReservedEndTime() != null) {
            reservation.setReservedEndTime(dto.getReservedEndTime());
        }
        if (dto.getRemark() != null) {
            reservation.setRemark(dto.getRemark());
        }
        if (dto.getOperator() != null) {
            reservation.setOperator(dto.getOperator());
        }

        return tableReservationRepository.save(reservation);
    }

    @Transactional(rollbackFor = Exception.class)
    public TableReservation confirmReservation(Long reservationId, Long storeId, Long operatorId) {
        TableReservation reservation = findReservationOrThrow(reservationId, storeId);

        if (!reservation.getStatus().equals(ReservationStatus.PENDING.getCode())) {
            throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(), "只有待确认的预定才能确认");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED.getCode());
        reservation.setOperator(operatorId);
        return tableReservationRepository.save(reservation);
    }

    @Transactional(rollbackFor = Exception.class)
    public TableReservation cancelReservation(Long reservationId, Long storeId, Long operatorId, String reason) {
        TableReservation reservation = findReservationOrThrow(reservationId, storeId);

        if (reservation.getStatus().equals(ReservationStatus.CANCELLED.getCode()) ||
                reservation.getStatus().equals(ReservationStatus.COMPLETED.getCode())) {
            throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(), "该预定已取消或已完成，无法再次取消");
        }

        reservation.setStatus(ReservationStatus.CANCELLED.getCode());
        reservation.setOperator(operatorId);
        if (reason != null) {
            reservation.setRemark(reason);
        }

        TableReservation saved = tableReservationRepository.save(reservation);

        List<TableReservation> activeReservations = tableReservationRepository
                .findByTableIdAndStatusOrderByReservedStartTimeAsc(
                        reservation.getTableId(), ReservationStatus.CONFIRMED.getCode());
        if (activeReservations.isEmpty()) {
            try {
                diningTableService.updateTableStatus(reservation.getTableId(), storeId, TableStatus.FREE.getCode());
            } catch (BusinessException ignored) {
            }
        }

        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public TableReservation seatReservation(Long reservationId, Long storeId, Long operatorId) {
        TableReservation reservation = findReservationOrThrow(reservationId, storeId);

        if (!reservation.getStatus().equals(ReservationStatus.CONFIRMED.getCode()) &&
                !reservation.getStatus().equals(ReservationStatus.PENDING.getCode())) {
            throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(), "只有已确认或待确认的预定才能到店");
        }

        reservation.setStatus(ReservationStatus.SEATED.getCode());
        reservation.setOperator(operatorId);
        TableReservation saved = tableReservationRepository.save(reservation);

        diningTableService.updateTableStatus(reservation.getTableId(), storeId, TableStatus.OCCUPIED.getCode());

        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public TableReservation completeReservation(Long reservationId, Long storeId, Long operatorId) {
        TableReservation reservation = findReservationOrThrow(reservationId, storeId);

        if (!reservation.getStatus().equals(ReservationStatus.SEATED.getCode())) {
            throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(), "只有已到店的预定才能完成");
        }

        reservation.setStatus(ReservationStatus.COMPLETED.getCode());
        reservation.setOperator(operatorId);
        TableReservation saved = tableReservationRepository.save(reservation);

        diningTableService.clearCurrentOrder(reservation.getTableId(), storeId);

        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public TableReservation markNoShow(Long reservationId, Long storeId, Long operatorId) {
        TableReservation reservation = findReservationOrThrow(reservationId, storeId);

        if (!reservation.getStatus().equals(ReservationStatus.CONFIRMED.getCode()) &&
                !reservation.getStatus().equals(ReservationStatus.PENDING.getCode())) {
            throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(), "只有已确认或待确认的预定才能标记未到店");
        }

        reservation.setStatus(ReservationStatus.NO_SHOW.getCode());
        reservation.setOperator(operatorId);
        TableReservation saved = tableReservationRepository.save(reservation);

        List<TableReservation> activeReservations = tableReservationRepository
                .findByTableIdAndStatusOrderByReservedStartTimeAsc(
                        reservation.getTableId(), ReservationStatus.CONFIRMED.getCode());
        if (activeReservations.isEmpty()) {
            try {
                diningTableService.updateTableStatus(reservation.getTableId(), storeId, TableStatus.FREE.getCode());
            } catch (BusinessException ignored) {
            }
        }

        return saved;
    }

    public TableReservation getReservation(Long reservationId, Long storeId) {
        return findReservationOrThrow(reservationId, storeId);
    }

    public Page<TableReservation> listReservationsByStore(Long storeId, int page, int size) {
        return tableReservationRepository.findByStoreIdOrderByReservedStartTime(
                storeId, PageRequest.of(page - 1, size, Sort.by(Sort.Order.asc("reservedStartTime"))));
    }

    public Page<TableReservation> listReservationsByStoreAndStatus(Long storeId, Integer status, int page, int size) {
        return tableReservationRepository.findByStoreIdAndStatusOrderByReservedStartTimeAsc(
                storeId, status, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "reservedStartTime")));
    }

    public List<TableReservation> listReservationsByTable(Long tableId, Integer status) {
        return tableReservationRepository.findByTableIdAndStatusOrderByReservedStartTimeAsc(tableId, status);
    }

    public Page<TableReservation> listReservationsByUser(Long userId, int page, int size) {
        return tableReservationRepository.findByUserIdOrderByReservedStartTimeDesc(
                userId, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "reservedStartTime")));
    }

    private TableReservation findReservationOrThrow(Long reservationId, Long storeId) {
        return tableReservationRepository.findByIdAndStoreId(reservationId, storeId)
                .orElseThrow(() -> new BusinessException(ReturnCodeEnum.NOT_FOUND.getCode(),
                        "预定记录不存在: " + reservationId));
    }
}
