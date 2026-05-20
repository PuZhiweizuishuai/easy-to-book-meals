package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.DiningTable;
import com.buguagaoshu.food.dto.CreateDiningTableDto;
import com.buguagaoshu.food.dto.UpdateDiningTableDto;
import com.buguagaoshu.food.enums.ReturnCodeEnum;
import com.buguagaoshu.food.enums.TableStatus;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.DiningTableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DiningTableService {
    private final DiningTableRepository diningTableRepository;

    private static final int NOT_DELETED = 0;
    private static final int DELETED = 1;

    public DiningTableService(DiningTableRepository diningTableRepository) {
        this.diningTableRepository = diningTableRepository;
    }

    /**
     * TODO 校验当前用户是否有在这个店面创建餐桌的权限
     * */
    @Transactional(rollbackFor = Exception.class)
    public DiningTable createTable(CreateDiningTableDto dto) {
        if (diningTableRepository.existsByStorefrontIdAndTableNumberAndDeleted(
                dto.getStorefrontId(), dto.getTableNumber(), NOT_DELETED)) {
            throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(),
                    "该门店已存在餐桌号: " + dto.getTableNumber());
        }

        DiningTable table = new DiningTable();
        table.setStorefrontId(dto.getStorefrontId());
        table.setTableNumber(dto.getTableNumber());
        table.setCapacity(dto.getCapacity());
        table.setStatus(dto.getStatus() != null ? dto.getStatus() : TableStatus.FREE.getCode());
        table.setArea(dto.getArea());
        table.setQrCodeKey(dto.getQrCodeKey() != null ? dto.getQrCodeKey() : generateQrCodeKey());
        table.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        table.setRemark(dto.getRemark());
        table.setCreatUser(dto.getCreatUser());
        table.setDeleted(NOT_DELETED);

        return diningTableRepository.save(table);
    }

    @Transactional(rollbackFor = Exception.class)
    public DiningTable updateTable(UpdateDiningTableDto dto) {
        DiningTable table = findTableOrThrow(dto.getId(), dto.getStorefrontId());

        if (dto.getTableNumber() != null && !dto.getTableNumber().equals(table.getTableNumber())) {
            if (diningTableRepository.existsByStorefrontIdAndTableNumberAndDeletedAndIdNot(
                    dto.getStorefrontId(), dto.getTableNumber(), NOT_DELETED, dto.getId())) {
                throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(),
                        "该门店已存在餐桌号: " + dto.getTableNumber());
            }
            table.setTableNumber(dto.getTableNumber());
        }

        if (dto.getCapacity() != null) {
            table.setCapacity(dto.getCapacity());
        }
        if (dto.getStatus() != null) {
            validateTableStatus(dto.getStatus());
            table.setStatus(dto.getStatus());
        }
        if (dto.getArea() != null) {
            table.setArea(dto.getArea());
        }
        if (dto.getQrCodeKey() != null) {
            table.setQrCodeKey(dto.getQrCodeKey());
        }
        if (dto.getSortOrder() != null) {
            table.setSortOrder(dto.getSortOrder());
        }
        if (dto.getRemark() != null) {
            table.setRemark(dto.getRemark());
        }

        return diningTableRepository.save(table);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTable(Long tableId, Long storefrontId) {
        DiningTable table = findTableOrThrow(tableId, storefrontId);
        table.setDeleted(DELETED);
        table.setStatus(TableStatus.DISABLED.getCode());
        diningTableRepository.save(table);
    }

    public DiningTable getTable(Long tableId, Long storefrontId) {
        return findTableOrThrow(tableId, storefrontId);
    }

    public List<DiningTable> listTablesByStore(Long storefrontId) {
        return diningTableRepository.findByStorefrontIdAndDeletedOrderBySortOrderAsc(storefrontId, NOT_DELETED);
    }

    public Page<DiningTable> listTablesByStore(Long storefrontId, int page, int size) {
        return diningTableRepository.findByStorefrontIdAndDeletedOrderBySortOrderAsc(
                storefrontId, NOT_DELETED, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "sortOrder")));
    }

    public List<DiningTable> listTablesByStoreAndStatus(Long storefrontId, Integer status) {
        return diningTableRepository.findByStorefrontIdAndStatusAndDeletedOrderBySortOrderAsc(
                storefrontId, status, NOT_DELETED);
    }

    public List<DiningTable> findAvailableTables(Long storefrontId, Integer minCapacity) {
        return diningTableRepository.findByStorefrontIdAndCapacityGreaterThanEqualAndDeleted(
                storefrontId, minCapacity);
    }

    @Transactional(rollbackFor = Exception.class)
    public DiningTable updateTableStatus(Long tableId, Long storefrontId, Integer status) {
        validateTableStatus(status);
        DiningTable table = findTableOrThrow(tableId, storefrontId);
        table.setStatus(status);
        return diningTableRepository.save(table);
    }

    @Transactional(rollbackFor = Exception.class)
    public DiningTable bindCurrentOrder(Long tableId, Long storefrontId, Long orderId) {
        DiningTable table = findTableOrThrow(tableId, storefrontId);
        table.setCurrentOrderId(orderId);
        table.setStatus(TableStatus.OCCUPIED.getCode());
        return diningTableRepository.save(table);
    }

    @Transactional(rollbackFor = Exception.class)
    public DiningTable clearCurrentOrder(Long tableId, Long storefrontId) {
        DiningTable table = findTableOrThrow(tableId, storefrontId);
        table.setCurrentOrderId(null);
        table.setStatus(TableStatus.CLEANING.getCode());
        return diningTableRepository.save(table);
    }

    private DiningTable findTableOrThrow(Long tableId, Long storefrontId) {
        DiningTable table = diningTableRepository.findByIdAndDeleted(tableId, NOT_DELETED)
                .orElseThrow(() -> new BusinessException(ReturnCodeEnum.NOT_FOUND.getCode(),
                        "餐桌不存在: " + tableId));
        if (!table.getStorefrontId().equals(storefrontId)) {
            throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(), "餐桌不属于该门店");
        }
        return table;
    }

    private void validateTableStatus(Integer status) {
        boolean valid = false;
        for (TableStatus ts : TableStatus.values()) {
            if (ts.getCode() == status) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new BusinessException(ReturnCodeEnum.PARAM_ERROR.getCode(), "无效的餐桌状态: " + status);
        }
    }

    private String generateQrCodeKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
