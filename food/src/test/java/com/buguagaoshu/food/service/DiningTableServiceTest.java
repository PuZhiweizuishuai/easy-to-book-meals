package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.DiningTable;
import com.buguagaoshu.food.dto.CreateDiningTableDto;
import com.buguagaoshu.food.dto.UpdateDiningTableDto;
import com.buguagaoshu.food.enums.TableStatus;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.DiningTableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiningTableServiceTest {

    @Mock
    private DiningTableRepository diningTableRepository;

    private DiningTableService diningTableService;

    private static final Long STORE_ID = 1L;
    private static final Long TABLE_ID = 100L;

    @BeforeEach
    void setUp() {
        diningTableService = new DiningTableService(diningTableRepository);
    }

    private DiningTable createTestTable() {
        DiningTable table = new DiningTable();
        table.setId(TABLE_ID);
        table.setStorefrontId(STORE_ID);
        table.setTableNumber("A1");
        table.setCapacity(4);
        table.setStatus(TableStatus.FREE.getCode());
        table.setArea("大厅");
        table.setDeleted(0);
        return table;
    }

    @Test
    void createTable_Success() {
        CreateDiningTableDto dto = new CreateDiningTableDto();
        dto.setStorefrontId(STORE_ID);
        dto.setTableNumber("A1");
        dto.setCapacity(4);
        dto.setArea("大厅");

        when(diningTableRepository.existsByStorefrontIdAndTableNumberAndDeleted(STORE_ID, "A1", 0))
                .thenReturn(false);
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> {
            DiningTable t = invocation.getArgument(0);
            t.setId(TABLE_ID);
            return t;
        });

        DiningTable result = diningTableService.createTable(dto);

        assertNotNull(result);
        assertEquals("A1", result.getTableNumber());
        assertEquals(4, result.getCapacity());
        assertEquals(TableStatus.FREE.getCode(), result.getStatus());
        assertEquals(0, result.getDeleted());
        assertNotNull(result.getQrCodeKey());
        verify(diningTableRepository).save(any(DiningTable.class));
    }

    @Test
    void createTable_DuplicateTableNumber_ThrowsException() {
        CreateDiningTableDto dto = new CreateDiningTableDto();
        dto.setStorefrontId(STORE_ID);
        dto.setTableNumber("A1");
        dto.setCapacity(4);

        when(diningTableRepository.existsByStorefrontIdAndTableNumberAndDeleted(STORE_ID, "A1", 0))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> diningTableService.createTable(dto));
        verify(diningTableRepository, never()).save(any());
    }

    @Test
    void updateTable_Success() {
        DiningTable existingTable = createTestTable();
        UpdateDiningTableDto dto = new UpdateDiningTableDto();
        dto.setId(TABLE_ID);
        dto.setStorefrontId(STORE_ID);
        dto.setCapacity(6);
        dto.setArea("包间");

        when(diningTableRepository.findByIdAndDeleted(TABLE_ID, 0)).thenReturn(Optional.of(existingTable));
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DiningTable result = diningTableService.updateTable(dto);

        assertEquals(6, result.getCapacity());
        assertEquals("包间", result.getArea());
        verify(diningTableRepository).save(any(DiningTable.class));
    }

    @Test
    void updateTable_ChangeTableNumber_Success() {
        DiningTable existingTable = createTestTable();
        UpdateDiningTableDto dto = new UpdateDiningTableDto();
        dto.setId(TABLE_ID);
        dto.setStorefrontId(STORE_ID);
        dto.setTableNumber("B1");

        when(diningTableRepository.findByIdAndDeleted(TABLE_ID, 0)).thenReturn(Optional.of(existingTable));
        when(diningTableRepository.existsByStorefrontIdAndTableNumberAndDeletedAndIdNot(STORE_ID, "B1", 0, TABLE_ID))
                .thenReturn(false);
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DiningTable result = diningTableService.updateTable(dto);

        assertEquals("B1", result.getTableNumber());
    }

    @Test
    void updateTable_DuplicateTableNumber_ThrowsException() {
        DiningTable existingTable = createTestTable();
        UpdateDiningTableDto dto = new UpdateDiningTableDto();
        dto.setId(TABLE_ID);
        dto.setStorefrontId(STORE_ID);
        dto.setTableNumber("B1");

        when(diningTableRepository.findByIdAndDeleted(TABLE_ID, 0)).thenReturn(Optional.of(existingTable));
        when(diningTableRepository.existsByStorefrontIdAndTableNumberAndDeletedAndIdNot(STORE_ID, "B1", 0, TABLE_ID))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> diningTableService.updateTable(dto));
        verify(diningTableRepository, never()).save(any());
    }

    @Test
    void updateTable_NotFound_ThrowsException() {
        UpdateDiningTableDto dto = new UpdateDiningTableDto();
        dto.setId(999L);
        dto.setStorefrontId(STORE_ID);

        when(diningTableRepository.findByIdAndDeleted(999L, 0)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> diningTableService.updateTable(dto));
    }

    @Test
    void deleteTable_Success() {
        DiningTable table = createTestTable();

        when(diningTableRepository.findByIdAndDeleted(TABLE_ID, 0)).thenReturn(Optional.of(table));
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        diningTableService.deleteTable(TABLE_ID, STORE_ID);

        verify(diningTableRepository).save(argThat(t ->
                t.getDeleted() == 1 && t.getStatus() == TableStatus.DISABLED.getCode()));
    }

    @Test
    void deleteTable_WrongStore_ThrowsException() {
        DiningTable table = createTestTable();

        when(diningTableRepository.findByIdAndDeleted(TABLE_ID, 0)).thenReturn(Optional.of(table));

        assertThrows(BusinessException.class, () -> diningTableService.deleteTable(TABLE_ID, 999L));
    }

    @Test
    void getTable_Success() {
        DiningTable table = createTestTable();

        when(diningTableRepository.findByIdAndDeleted(TABLE_ID, 0)).thenReturn(Optional.of(table));

        DiningTable result = diningTableService.getTable(TABLE_ID, STORE_ID);

        assertNotNull(result);
        assertEquals(TABLE_ID, result.getId());
    }

    @Test
    void getTable_NotFound_ThrowsException() {
        when(diningTableRepository.findByIdAndDeleted(999L, 0)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> diningTableService.getTable(999L, STORE_ID));
    }

    @Test
    void listTablesByStore_Success() {
        List<DiningTable> tables = List.of(createTestTable());

        when(diningTableRepository.findByStorefrontIdAndDeletedOrderBySortOrderAsc(STORE_ID, 0))
                .thenReturn(tables);

        List<DiningTable> result = diningTableService.listTablesByStore(STORE_ID);

        assertEquals(1, result.size());
    }

    @Test
    void listTablesByStore_Paginated_Success() {
        Page<DiningTable> page = new PageImpl<>(List.of(createTestTable()));

        when(diningTableRepository.findByStorefrontIdAndDeletedOrderBySortOrderAsc(
                eq(STORE_ID), eq(0), any(Pageable.class))).thenReturn(page);

        Page<DiningTable> result = diningTableService.listTablesByStore(STORE_ID, 1, 10);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void updateTableStatus_Success() {
        DiningTable table = createTestTable();

        when(diningTableRepository.findByIdAndDeleted(TABLE_ID, 0)).thenReturn(Optional.of(table));
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DiningTable result = diningTableService.updateTableStatus(TABLE_ID, STORE_ID, TableStatus.OCCUPIED.getCode());

        assertEquals(TableStatus.OCCUPIED.getCode(), result.getStatus());
    }

    @Test
    void updateTableStatus_InvalidStatus_ThrowsException() {
        assertThrows(BusinessException.class,
                () -> diningTableService.updateTableStatus(TABLE_ID, STORE_ID, 99));
    }

    @Test
    void bindCurrentOrder_Success() {
        DiningTable table = createTestTable();
        Long orderId = 200L;

        when(diningTableRepository.findByIdAndDeleted(TABLE_ID, 0)).thenReturn(Optional.of(table));
        // 原子条件 UPDATE 抢占成功，并模拟数据库已写入新状态
        when(diningTableRepository.bindOrderIfFree(eq(TABLE_ID), eq(orderId),
                eq(TableStatus.OCCUPIED.getCode()), eq(TableStatus.DISABLED.getCode()), anyLong()))
                .thenAnswer(invocation -> {
                    table.setCurrentOrderId(orderId);
                    table.setStatus(TableStatus.OCCUPIED.getCode());
                    return 1;
                });

        DiningTable result = diningTableService.bindCurrentOrder(TABLE_ID, STORE_ID, orderId);

        assertEquals(orderId, result.getCurrentOrderId());
        assertEquals(TableStatus.OCCUPIED.getCode(), result.getStatus());
        verify(diningTableRepository, never()).save(any(DiningTable.class));
    }

    @Test
    void bindCurrentOrder_AlreadyOccupied_ThrowsException() {
        DiningTable table = createTestTable();
        table.setCurrentOrderId(999L);
        table.setStatus(TableStatus.OCCUPIED.getCode());
        Long orderId = 200L;

        when(diningTableRepository.findByIdAndDeleted(TABLE_ID, 0)).thenReturn(Optional.of(table));
        // 并发场景：条件 UPDATE 抢占失败（该桌已绑定其他订单）
        when(diningTableRepository.bindOrderIfFree(eq(TABLE_ID), eq(orderId),
                eq(TableStatus.OCCUPIED.getCode()), eq(TableStatus.DISABLED.getCode()), anyLong()))
                .thenReturn(0);

        assertThrows(BusinessException.class,
                () -> diningTableService.bindCurrentOrder(TABLE_ID, STORE_ID, orderId));
        // 原绑定订单不应被覆盖
        assertEquals(999L, table.getCurrentOrderId());
    }

    @Test
    void clearCurrentOrder_Success() {
        DiningTable table = createTestTable();
        table.setCurrentOrderId(200L);
        table.setStatus(TableStatus.OCCUPIED.getCode());

        when(diningTableRepository.findByIdAndDeleted(TABLE_ID, 0)).thenReturn(Optional.of(table));
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DiningTable result = diningTableService.clearCurrentOrder(TABLE_ID, STORE_ID);

        assertNull(result.getCurrentOrderId());
        assertEquals(TableStatus.CLEANING.getCode(), result.getStatus());
    }
}
