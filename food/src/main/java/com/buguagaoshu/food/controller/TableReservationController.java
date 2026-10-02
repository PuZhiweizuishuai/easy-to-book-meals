package com.buguagaoshu.food.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.dto.CreateReservationDto;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.dto.UpdateReservationDto;
import com.buguagaoshu.food.service.TableReservationService;
import com.buguagaoshu.food.utils.StoreAuthHelper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 餐桌预定控制器
 * <p>
 * 权限策略：
 * - 门店维度的预定管理（创建/确认/取消/落座/完成/查询）：需登录 + 对应门店 table:manage / table:view 权限
 * - 操作人统一取当前登录用户，不信任前端传入的 operatorId
 */
@RestController
@RequestMapping("/api/table-reservation")
@Validated
public class TableReservationController {
    private final TableReservationService tableReservationService;
    private final StoreAuthHelper storeAuthHelper;

    public TableReservationController(TableReservationService tableReservationService,
                                      StoreAuthHelper storeAuthHelper) {
        this.tableReservationService = tableReservationService;
        this.storeAuthHelper = storeAuthHelper;
    }

    /**
     * 创建预定（电话/到店散客代录）
     */
    @SaCheckLogin
    @PostMapping("/add")
    public ResponseDetails createReservation(@Valid @RequestBody CreateReservationDto dto) {
        storeAuthHelper.checkStorePermission(dto.getStoreId(), "table:manage");
        // 操作人取当前登录用户，不信任前端数据
        dto.setOperator(StpUtil.getLoginIdAsLong());
        return ResponseDetails.ok().put("data", tableReservationService.createReservation(dto));
    }

    /**
     * 更新预定
     * */
    @SaCheckLogin
    @PostMapping("/update")
    public ResponseDetails updateReservation(@Valid @RequestBody UpdateReservationDto dto) {
        storeAuthHelper.checkStorePermission(dto.getStoreId(), "table:manage");
        dto.setOperator(StpUtil.getLoginIdAsLong());
        return ResponseDetails.ok().put("data", tableReservationService.updateReservation(dto));
    }

    /**
     * 确认预定
     * */
    @SaCheckLogin
    @PostMapping("/confirm/{id}")
    public ResponseDetails confirmReservation(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        storeAuthHelper.checkStorePermission(storeId, "table:manage");
        return ResponseDetails.ok().put("data",
                tableReservationService.confirmReservation(id, storeId, StpUtil.getLoginIdAsLong()));
    }

    /**
     * 取消预定
     * */
    @SaCheckLogin
    @PostMapping("/cancel/{id}")
    public ResponseDetails cancelReservation(
            @PathVariable Long id,
            @RequestParam Long storeId,
            @RequestParam(value = "reason", required = false) String reason) {
        storeAuthHelper.checkStorePermission(storeId, "table:manage");
        return ResponseDetails.ok().put("data",
                tableReservationService.cancelReservation(id, storeId, StpUtil.getLoginIdAsLong(), reason));
    }


    /**
     * 到店落座
     * */
    @SaCheckLogin
    @PostMapping("/seat/{id}")
    public ResponseDetails seatReservation(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        storeAuthHelper.checkStorePermission(storeId, "table:manage");
        return ResponseDetails.ok().put("data",
                tableReservationService.seatReservation(id, storeId, StpUtil.getLoginIdAsLong()));
    }

    /**
     * 完成就餐
     * */
    @SaCheckLogin
    @PostMapping("/complete/{id}")
    public ResponseDetails completeReservation(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        storeAuthHelper.checkStorePermission(storeId, "table:manage");
        return ResponseDetails.ok().put("data",
                tableReservationService.completeReservation(id, storeId, StpUtil.getLoginIdAsLong()));
    }

    /**
     * 标记未到店
     * */
    @SaCheckLogin
    @PostMapping("/no-show/{id}")
    public ResponseDetails markNoShow(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        storeAuthHelper.checkStorePermission(storeId, "table:manage");
        return ResponseDetails.ok().put("data",
                tableReservationService.markNoShow(id, storeId, StpUtil.getLoginIdAsLong()));
    }


    /**
     * 查询预定详情
     * */
    @SaCheckLogin
    @GetMapping("/{id}")
    public ResponseDetails getReservation(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        storeAuthHelper.checkStorePermission(storeId, "table:view");
        return ResponseDetails.ok().put("data", tableReservationService.getReservation(id, storeId));
    }


    /**
     * 按门店分页查询
     * */
    @SaCheckLogin
    @GetMapping("/list")
    public ResponseDetails listReservationsByStore(
            @RequestParam Long storeId,
            @RequestParam(value = "page", required = false, defaultValue = "1")
            @Min(value = 1, message = "页码不能小于1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10")
            @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 100, message = "每页条数不能超过100") Integer size) {
        storeAuthHelper.checkStorePermission(storeId, "table:view");
        return ResponseDetails.ok().put("data", tableReservationService.listReservationsByStore(storeId, page, size));
    }


    /**
     * 按门店+日期范围分页查询
     * */
    @SaCheckLogin
    @GetMapping("/list/date")
    public ResponseDetails listReservationsByDate(
            @RequestParam Long storeId,
            @RequestParam Long startTime,
            @RequestParam Long endTime,
            @RequestParam(value = "page", required = false, defaultValue = "1")
            @Min(value = 1, message = "页码不能小于1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10")
            @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 100, message = "每页条数不能超过100") Integer size) {
        storeAuthHelper.checkStorePermission(storeId, "table:view");
        return ResponseDetails.ok().put("data", tableReservationService.listReservationsByStoreAndDateRange(storeId, startTime, endTime, page, size));
    }

    /**
     * 按门店+状态查询
     * */
    @SaCheckLogin
    @GetMapping("/list/status")
    public ResponseDetails listReservationsByStatus(
            @RequestParam Long storeId,
            @RequestParam Integer status,
            @RequestParam(value = "page", required = false, defaultValue = "1")
            @Min(value = 1, message = "页码不能小于1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10")
            @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 100, message = "每页条数不能超过100") Integer size) {
        storeAuthHelper.checkStorePermission(storeId, "table:view");
        return ResponseDetails.ok().put("data", tableReservationService.listReservationsByStoreAndStatus(storeId, status, page, size));
    }


    /**
     * 按餐桌查询
     * */
    @SaCheckLogin
    @GetMapping("/list/table")
    public ResponseDetails listReservationsByTable(
            @RequestParam Long tableId,
            @RequestParam Long storeId,
            @RequestParam Integer status) {
        storeAuthHelper.checkStorePermission(storeId, "table:view");
        return ResponseDetails.ok().put("data",
                tableReservationService.listReservationsByTable(tableId, storeId, status));
    }


    /**
     * 按用户查询（顾客查询自己的预定）
     * */
    @SaCheckLogin
    @GetMapping("/list/user")
    public ResponseDetails listReservationsByUser(
            @RequestParam(required = false) Long userId,
            @RequestParam(value = "page", required = false, defaultValue = "1")
            @Min(value = 1, message = "页码不能小于1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10")
            @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 100, message = "每页条数不能超过100") Integer size) {
        // 顾客只能查询自己的预定；门店员工可按 userId 查询指定用户的预定记录
        Long queryUserId = userId;
        if (queryUserId == null || !queryUserId.equals(StpUtil.getLoginIdAsLong())) {
            queryUserId = StpUtil.getLoginIdAsLong();
        }
        return ResponseDetails.ok().put("data", tableReservationService.listReservationsByUser(queryUserId, page, size));
    }
}
