package com.buguagaoshu.food.controller;

import com.buguagaoshu.food.dto.CreateReservationDto;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.dto.UpdateReservationDto;
import com.buguagaoshu.food.service.TableReservationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/table-reservation")
@Validated
public class TableReservationController {
    private final TableReservationService tableReservationService;

    public TableReservationController(TableReservationService tableReservationService) {
        this.tableReservationService = tableReservationService;
    }

    @PostMapping("/add")
    public ResponseDetails createReservation(@Valid @RequestBody CreateReservationDto dto) {
        return ResponseDetails.ok().put("data", tableReservationService.createReservation(dto));
    }

    @PostMapping("/update")
    public ResponseDetails updateReservation(@Valid @RequestBody UpdateReservationDto dto) {
        return ResponseDetails.ok().put("data", tableReservationService.updateReservation(dto));
    }

    /**
     * 确认预定
     * */
    @PostMapping("/confirm/{id}")
    public ResponseDetails confirmReservation(
            @PathVariable Long id,
            @RequestParam Long storeId,
            @RequestParam Long operatorId) {
        return ResponseDetails.ok().put("data", tableReservationService.confirmReservation(id, storeId, 0L));
    }

    /**
     * 取消预定
     * */
    @PostMapping("/cancel/{id}")
    public ResponseDetails cancelReservation(
            @PathVariable Long id,
            @RequestParam Long storeId,
            @RequestParam Long operatorId,
            @RequestParam(value = "reason", required = false) String reason) {
        return ResponseDetails.ok().put("data", tableReservationService.cancelReservation(id, storeId, operatorId, reason));
    }


    /**
     * 到店落座
     * */
    @PostMapping("/seat/{id}")
    public ResponseDetails seatReservation(
            @PathVariable Long id,
            @RequestParam Long storeId,
            @RequestParam Long operatorId) {
        return ResponseDetails.ok().put("data", tableReservationService.seatReservation(id, storeId, operatorId));
    }

    /**
     * 完成就餐
     * */
    @PostMapping("/complete/{id}")
    public ResponseDetails completeReservation(
            @PathVariable Long id,
            @RequestParam Long storeId,
            @RequestParam Long operatorId) {
        return ResponseDetails.ok().put("data", tableReservationService.completeReservation(id, storeId, operatorId));
    }

    /**
     * 标记未到店
     * */
    @PostMapping("/no-show/{id}")
    public ResponseDetails markNoShow(
            @PathVariable Long id,
            @RequestParam Long storeId,
            @RequestParam Long operatorId) {
        return ResponseDetails.ok().put("data", tableReservationService.markNoShow(id, storeId, operatorId));
    }


    /**
     * 查询预定详情
     * */
    @GetMapping("/{id}")
    public ResponseDetails getReservation(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        return ResponseDetails.ok().put("data", tableReservationService.getReservation(id, storeId));
    }


    /**
     * 按门店分页查询
     * */
    @GetMapping("/list")
    public ResponseDetails listReservationsByStore(
            @RequestParam Long storeId,
            @RequestParam(value = "page", required = false, defaultValue = "1")
            @Min(value = 1, message = "页码不能小于1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10")
            @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 100, message = "每页条数不能超过100") Integer size) {
        return ResponseDetails.ok().put("data", tableReservationService.listReservationsByStore(storeId, page, size));
    }


    /**
     * 按门店+状态查询
     * */
    @GetMapping("/list/status")
    public ResponseDetails listReservationsByStatus(
            @RequestParam Long storeId,
            @RequestParam Integer status,
            @RequestParam(value = "page", required = false, defaultValue = "1")
            @Min(value = 1, message = "页码不能小于1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10")
            @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 100, message = "每页条数不能超过100") Integer size) {
        return ResponseDetails.ok().put("data", tableReservationService.listReservationsByStoreAndStatus(storeId, status, page, size));
    }


    /**
     * 按餐桌查询
     * */
    @GetMapping("/list/table")
    public ResponseDetails listReservationsByTable(
            @RequestParam Long tableId,
            @RequestParam Integer status) {
        return ResponseDetails.ok().put("data", tableReservationService.listReservationsByTable(tableId, status));
    }

    /**
     * 按用户查询
     * */
    @GetMapping("/list/user")
    public ResponseDetails listReservationsByUser(
            @RequestParam Long userId,
            @RequestParam(value = "page", required = false, defaultValue = "1")
            @Min(value = 1, message = "页码不能小于1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10")
            @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 100, message = "每页条数不能超过100") Integer size) {
        return ResponseDetails.ok().put("data", tableReservationService.listReservationsByUser(userId, page, size));
    }
}
