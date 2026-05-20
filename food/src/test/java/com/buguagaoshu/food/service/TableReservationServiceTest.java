package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.DiningTable;
import com.buguagaoshu.food.data.TableReservation;
import com.buguagaoshu.food.dto.CreateReservationDto;
import com.buguagaoshu.food.dto.UpdateReservationDto;
import com.buguagaoshu.food.enums.ReservationStatus;
import com.buguagaoshu.food.enums.TableStatus;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.TableReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TableReservationServiceTest {

    @Mock
    private TableReservationRepository tableReservationRepository;

    @Mock
    private DiningTableService diningTableService;

    private TableReservationService tableReservationService;

    private static final Long STORE_ID = 1L;
    private static final Long TABLE_ID = 100L;
    private static final Long RESERVATION_ID = 200L;
    private static final Long OPERATOR_ID = 10L;
    private static final Long FUTURE_START = System.currentTimeMillis() + 3600000;
    private static final Long FUTURE_END = FUTURE_START + 7200000;

    @BeforeEach
    void setUp() {
        tableReservationService = new TableReservationService(tableReservationRepository, diningTableService);
    }

    private DiningTable createTestTable() {
        DiningTable table = new DiningTable();
        table.setId(TABLE_ID);
        table.setStorefrontId(STORE_ID);
        table.setTableNumber("A1");
        table.setCapacity(4);
        table.setStatus(TableStatus.FREE.getCode());
        table.setDeleted(0);
        return table;
    }

    private TableReservation createTestReservation() {
        TableReservation reservation = new TableReservation();
        reservation.setId(RESERVATION_ID);
        reservation.setStoreId(STORE_ID);
        reservation.setTableId(TABLE_ID);
        reservation.setContactName("张三");
        reservation.setContactPhone("13800138000");
        reservation.setGuestCount(3);
        reservation.setStatus(ReservationStatus.PENDING.getCode());
        reservation.setReservedStartTime(FUTURE_START);
        reservation.setReservedEndTime(FUTURE_END);
        return reservation;
    }

    private CreateReservationDto createTestCreateDto() {
        CreateReservationDto dto = new CreateReservationDto();
        dto.setStoreId(STORE_ID);
        dto.setTableId(TABLE_ID);
        dto.setContactName("张三");
        dto.setContactPhone("13800138000");
        dto.setGuestCount(3);
        dto.setReservedStartTime(FUTURE_START);
        dto.setReservedEndTime(FUTURE_END);
        dto.setOperator(OPERATOR_ID);
        return dto;
    }

    @Test
    void createReservation_Success() {
        CreateReservationDto dto = createTestCreateDto();
        DiningTable table = createTestTable();

        when(diningTableService.getTable(TABLE_ID, STORE_ID)).thenReturn(table);
        when(tableReservationRepository.findConflictingReservations(
                eq(TABLE_ID), anyList(), eq(FUTURE_START), eq(FUTURE_END)))
                .thenReturn(Collections.emptyList());
        when(tableReservationRepository.save(any(TableReservation.class))).thenAnswer(invocation -> {
            TableReservation r = invocation.getArgument(0);
            r.setId(RESERVATION_ID);
            return r;
        });
        when(diningTableService.updateTableStatus(TABLE_ID, STORE_ID, TableStatus.RESERVED.getCode()))
                .thenReturn(table);

        TableReservation result = tableReservationService.createReservation(dto);

        assertNotNull(result);
        assertEquals(ReservationStatus.PENDING.getCode(), result.getStatus());
        assertEquals("张三", result.getContactName());
        verify(tableReservationRepository).save(any(TableReservation.class));
    }

    @Test
    void createReservation_StartTimeAfterEndTime_ThrowsException() {
        CreateReservationDto dto = createTestCreateDto();
        dto.setReservedStartTime(FUTURE_END);
        dto.setReservedEndTime(FUTURE_START);

        assertThrows(BusinessException.class, () -> tableReservationService.createReservation(dto));
    }

    @Test
    void createReservation_StartTimeInPast_ThrowsException() {
        CreateReservationDto dto = createTestCreateDto();
        dto.setReservedStartTime(System.currentTimeMillis() - 10000);
        dto.setReservedEndTime(System.currentTimeMillis() + 3600000);

        assertThrows(BusinessException.class, () -> tableReservationService.createReservation(dto));
    }

    @Test
    void createReservation_GuestCountExceedsCapacity_ThrowsException() {
        CreateReservationDto dto = createTestCreateDto();
        dto.setGuestCount(10);
        DiningTable table = createTestTable();

        when(diningTableService.getTable(TABLE_ID, STORE_ID)).thenReturn(table);

        assertThrows(BusinessException.class, () -> tableReservationService.createReservation(dto));
    }

    @Test
    void createReservation_TimeConflict_ThrowsException() {
        CreateReservationDto dto = createTestCreateDto();
        DiningTable table = createTestTable();

        when(diningTableService.getTable(TABLE_ID, STORE_ID)).thenReturn(table);
        when(tableReservationRepository.findConflictingReservations(
                eq(TABLE_ID), anyList(), eq(FUTURE_START), eq(FUTURE_END)))
                .thenReturn(List.of(createTestReservation()));

        assertThrows(BusinessException.class, () -> tableReservationService.createReservation(dto));
    }

    @Test
    void confirmReservation_Success() {
        TableReservation reservation = createTestReservation();

        when(tableReservationRepository.findByIdAndStoreId(RESERVATION_ID, STORE_ID))
                .thenReturn(Optional.of(reservation));
        when(tableReservationRepository.save(any(TableReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TableReservation result = tableReservationService.confirmReservation(RESERVATION_ID, STORE_ID, OPERATOR_ID);

        assertEquals(ReservationStatus.CONFIRMED.getCode(), result.getStatus());
        assertEquals(OPERATOR_ID, result.getOperator());
    }

    @Test
    void confirmReservation_NotPending_ThrowsException() {
        TableReservation reservation = createTestReservation();
        reservation.setStatus(ReservationStatus.CONFIRMED.getCode());

        when(tableReservationRepository.findByIdAndStoreId(RESERVATION_ID, STORE_ID))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessException.class,
                () -> tableReservationService.confirmReservation(RESERVATION_ID, STORE_ID, OPERATOR_ID));
    }

    @Test
    void cancelReservation_Success() {
        TableReservation reservation = createTestReservation();
        reservation.setStatus(ReservationStatus.CONFIRMED.getCode());

        when(tableReservationRepository.findByIdAndStoreId(RESERVATION_ID, STORE_ID))
                .thenReturn(Optional.of(reservation));
        when(tableReservationRepository.save(any(TableReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tableReservationRepository.findByTableIdAndStatusOrderByReservedStartTimeAsc(
                eq(TABLE_ID), eq(ReservationStatus.CONFIRMED.getCode())))
                .thenReturn(Collections.emptyList());

        DiningTable table = createTestTable();
        when(diningTableService.updateTableStatus(TABLE_ID, STORE_ID, TableStatus.FREE.getCode()))
                .thenReturn(table);

        TableReservation result = tableReservationService.cancelReservation(RESERVATION_ID, STORE_ID, OPERATOR_ID, "临时有事");

        assertEquals(ReservationStatus.CANCELLED.getCode(), result.getStatus());
    }

    @Test
    void cancelReservation_AlreadyCancelled_ThrowsException() {
        TableReservation reservation = createTestReservation();
        reservation.setStatus(ReservationStatus.CANCELLED.getCode());

        when(tableReservationRepository.findByIdAndStoreId(RESERVATION_ID, STORE_ID))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessException.class,
                () -> tableReservationService.cancelReservation(RESERVATION_ID, STORE_ID, OPERATOR_ID, null));
    }

    @Test
    void seatReservation_Success() {
        TableReservation reservation = createTestReservation();
        reservation.setStatus(ReservationStatus.CONFIRMED.getCode());

        when(tableReservationRepository.findByIdAndStoreId(RESERVATION_ID, STORE_ID))
                .thenReturn(Optional.of(reservation));
        when(tableReservationRepository.save(any(TableReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        DiningTable table = createTestTable();
        when(diningTableService.updateTableStatus(TABLE_ID, STORE_ID, TableStatus.OCCUPIED.getCode()))
                .thenReturn(table);

        TableReservation result = tableReservationService.seatReservation(RESERVATION_ID, STORE_ID, OPERATOR_ID);

        assertEquals(ReservationStatus.SEATED.getCode(), result.getStatus());
        verify(diningTableService).updateTableStatus(TABLE_ID, STORE_ID, TableStatus.OCCUPIED.getCode());
    }

    @Test
    void completeReservation_Success() {
        TableReservation reservation = createTestReservation();
        reservation.setStatus(ReservationStatus.SEATED.getCode());

        when(tableReservationRepository.findByIdAndStoreId(RESERVATION_ID, STORE_ID))
                .thenReturn(Optional.of(reservation));
        when(tableReservationRepository.save(any(TableReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        DiningTable table = createTestTable();
        when(diningTableService.clearCurrentOrder(TABLE_ID, STORE_ID)).thenReturn(table);

        TableReservation result = tableReservationService.completeReservation(RESERVATION_ID, STORE_ID, OPERATOR_ID);

        assertEquals(ReservationStatus.COMPLETED.getCode(), result.getStatus());
        verify(diningTableService).clearCurrentOrder(TABLE_ID, STORE_ID);
    }

    @Test
    void completeReservation_NotSeated_ThrowsException() {
        TableReservation reservation = createTestReservation();
        reservation.setStatus(ReservationStatus.CONFIRMED.getCode());

        when(tableReservationRepository.findByIdAndStoreId(RESERVATION_ID, STORE_ID))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessException.class,
                () -> tableReservationService.completeReservation(RESERVATION_ID, STORE_ID, OPERATOR_ID));
    }

    @Test
    void markNoShow_Success() {
        TableReservation reservation = createTestReservation();
        reservation.setStatus(ReservationStatus.CONFIRMED.getCode());

        when(tableReservationRepository.findByIdAndStoreId(RESERVATION_ID, STORE_ID))
                .thenReturn(Optional.of(reservation));
        when(tableReservationRepository.save(any(TableReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tableReservationRepository.findByTableIdAndStatusOrderByReservedStartTimeAsc(
                eq(TABLE_ID), eq(ReservationStatus.CONFIRMED.getCode())))
                .thenReturn(Collections.emptyList());
        DiningTable table = createTestTable();
        when(diningTableService.updateTableStatus(TABLE_ID, STORE_ID, TableStatus.FREE.getCode()))
                .thenReturn(table);

        TableReservation result = tableReservationService.markNoShow(RESERVATION_ID, STORE_ID, OPERATOR_ID);

        assertEquals(ReservationStatus.NO_SHOW.getCode(), result.getStatus());
    }

    @Test
    void getReservation_Success() {
        TableReservation reservation = createTestReservation();

        when(tableReservationRepository.findByIdAndStoreId(RESERVATION_ID, STORE_ID))
                .thenReturn(Optional.of(reservation));

        TableReservation result = tableReservationService.getReservation(RESERVATION_ID, STORE_ID);

        assertNotNull(result);
        assertEquals(RESERVATION_ID, result.getId());
    }

    @Test
    void getReservation_NotFound_ThrowsException() {
        when(tableReservationRepository.findByIdAndStoreId(999L, STORE_ID))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> tableReservationService.getReservation(999L, STORE_ID));
    }

    @Test
    void updateReservation_Success() {
        TableReservation reservation = createTestReservation();
        UpdateReservationDto dto = new UpdateReservationDto();
        dto.setId(RESERVATION_ID);
        dto.setStoreId(STORE_ID);
        dto.setContactName("李四");
        dto.setGuestCount(2);

        when(tableReservationRepository.findByIdAndStoreId(RESERVATION_ID, STORE_ID))
                .thenReturn(Optional.of(reservation));
        when(tableReservationRepository.save(any(TableReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TableReservation result = tableReservationService.updateReservation(dto);

        assertEquals("李四", result.getContactName());
        assertEquals(2, result.getGuestCount());
    }

    @Test
    void updateReservation_CancelledReservation_ThrowsException() {
        TableReservation reservation = createTestReservation();
        reservation.setStatus(ReservationStatus.CANCELLED.getCode());
        UpdateReservationDto dto = new UpdateReservationDto();
        dto.setId(RESERVATION_ID);
        dto.setStoreId(STORE_ID);

        when(tableReservationRepository.findByIdAndStoreId(RESERVATION_ID, STORE_ID))
                .thenReturn(Optional.of(reservation));

        assertThrows(BusinessException.class, () -> tableReservationService.updateReservation(dto));
    }
}
