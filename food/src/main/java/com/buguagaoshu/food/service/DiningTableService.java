package com.buguagaoshu.food.service;

import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.data.DiningTable;
import com.buguagaoshu.food.dto.CreateDiningTableDto;
import com.buguagaoshu.food.dto.UpdateDiningTableDto;
import com.buguagaoshu.food.enums.ReturnCodeEnum;
import com.buguagaoshu.food.enums.TableStatus;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.DiningTableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DiningTableService {
    private static final Logger logger = LoggerFactory.getLogger(DiningTableService.class);

    /** 清理中超时阈值（10 分钟） */
    private static final long CLEANING_TIMEOUT_MS = 10 * 60 * 1000L;

    private final DiningTableRepository diningTableRepository;

    private static final int NOT_DELETED = 0;
    private static final int DELETED = 1;

    public DiningTableService(DiningTableRepository diningTableRepository) {
        this.diningTableRepository = diningTableRepository;
    }

    /**
     * 创建餐桌
     * 创建人由系统记录为当前登录用户，不信任前端传入
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
        table.setQrCodeKey(dto.getQrCodeKey() != null && !dto.getQrCodeKey().isBlank()
                ? dto.getQrCodeKey() : generateQrCodeKey());
        table.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        table.setRemark(dto.getRemark());
        table.setCreatUser(StpUtil.getLoginIdAsLong());
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

    /**
     * 通过二维码标识查询餐桌（顾客扫码点餐入口）
     */
    public DiningTable getTableByQrCode(String qrCodeKey) {
        return diningTableRepository.findByQrCodeKeyAndDeleted(qrCodeKey, NOT_DELETED)
                .orElseThrow(() -> new BusinessException(ReturnCodeEnum.NOT_FOUND.getCode(),
                        "餐桌不存在或二维码已失效"));
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

    /**
     * 绑定餐桌当前订单（原子操作，防止同桌并发下单互相覆盖）
     * <p>
     * 通过条件 UPDATE（currentOrderId IS NULL）抢占餐桌：
     * 抢占失败说明该桌已有进行中的订单，抛出异常使整个下单事务回滚（库存随之回滚）。
     */
    @Transactional(rollbackFor = Exception.class)
    public DiningTable bindCurrentOrder(Long tableId, Long storefrontId, Long orderId) {
        DiningTable table = findTableOrThrow(tableId, storefrontId);
        int affected = diningTableRepository.bindOrderIfFree(
                table.getId(), orderId,
                TableStatus.OCCUPIED.getCode(),
                TableStatus.DISABLED.getCode(),
                System.currentTimeMillis());
        if (affected == 0) {
            throw new BusinessException(409, "该餐桌已有进行中的订单，可直接在本桌订单中加菜");
        }
        // 批量 UPDATE 后持久化上下文已清空，重新读取返回最新状态
        return diningTableRepository.findByIdAndDeleted(tableId, NOT_DELETED)
                .orElseThrow(() -> new BusinessException(ReturnCodeEnum.NOT_FOUND.getCode(),
                        "餐桌不存在: " + tableId));
    }

    @Transactional(rollbackFor = Exception.class)
    public DiningTable clearCurrentOrder(Long tableId, Long storefrontId) {
        return clearCurrentOrder(tableId, storefrontId, TableStatus.CLEANING.getCode());
    }

    /**
     * 清空餐桌当前订单并设置目标状态。
     * 取消订单（未真正就餐）→ FREE；订单完成（客人离席）→ CLEANING。
     */
    @Transactional(rollbackFor = Exception.class)
    public DiningTable clearCurrentOrder(Long tableId, Long storefrontId, Integer targetStatus) {
        DiningTable table = findTableOrThrow(tableId, storefrontId);
        table.setCurrentOrderId(null);
        table.setStatus(targetStatus);
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

    /**
     * 定时任务：清理中超时（10 分钟）的餐桌自动复位为空闲。
     * 每 60 秒扫描一次，避免服务员遗漏手动复位。
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional(rollbackFor = Exception.class)
    public void autoResetCleaningTables() {
        long threshold = System.currentTimeMillis() - CLEANING_TIMEOUT_MS;
        List<DiningTable> timeoutTables = diningTableRepository
                .findByStatusAndUpdateTimeBefore(TableStatus.CLEANING.getCode(), threshold);
        for (DiningTable table : timeoutTables) {
            table.setStatus(TableStatus.FREE.getCode());
            diningTableRepository.save(table);
            logger.info("餐桌清理超时自动复位: tableId={}, tableNumber={}, storeId={}",
                    table.getId(), table.getTableNumber(), table.getStorefrontId());
        }
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
