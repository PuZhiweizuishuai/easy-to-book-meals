package com.buguagaoshu.food.controller;

import com.buguagaoshu.food.dto.CreateDiningTableDto;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.dto.UpdateDiningTableDto;
import com.buguagaoshu.food.service.DiningTableService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dining-table")
@Validated
public class DiningTableController {
    private final DiningTableService diningTableService;

    public DiningTableController(DiningTableService diningTableService) {
        this.diningTableService = diningTableService;
    }

    /**
     * 创建餐桌
     * */
    @PostMapping("/add")
    public ResponseDetails createTable(@Valid @RequestBody CreateDiningTableDto dto) {
        return ResponseDetails.ok().put("data", diningTableService.createTable(dto));
    }

    /**
     * 更新餐桌
     * */
    @PostMapping("/update")
    public ResponseDetails updateTable(@Valid @RequestBody UpdateDiningTableDto dto) {
        return ResponseDetails.ok().put("data", diningTableService.updateTable(dto));
    }

    /**
     * 逻辑删除餐桌
     * */
    @PostMapping("/delete/{id}")
    public ResponseDetails deleteTable(@PathVariable Long id,
                                       @RequestParam Long storefrontId) {
        diningTableService.deleteTable(id, storefrontId);
        return ResponseDetails.ok("删除成功");
    }


    /**
     * 查询单个餐桌
     * */
    @GetMapping("/{id}")
    public ResponseDetails getTable(@PathVariable Long id,
                                    @RequestParam Long storefrontId) {
        return ResponseDetails.ok().put("data", diningTableService.getTable(id, storefrontId));
    }


    /**
     * 分页查询餐桌列表
     * */
    @GetMapping("/list")
    public ResponseDetails listTables(
            @RequestParam Long storefrontId,
            @RequestParam(value = "page", required = false, defaultValue = "1")
            @Min(value = 1, message = "页码不能小于1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10")
            @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 100, message = "每页条数不能超过100") Integer size) {
        return ResponseDetails.ok().put("data", diningTableService.listTablesByStore(storefrontId, page, size));
    }


    /**
     * 查询全部餐桌
     * */
    @GetMapping("/list/all")
    public ResponseDetails listAllTables(@RequestParam Long storefrontId) {
        return ResponseDetails.ok().put("data", diningTableService.listTablesByStore(storefrontId));
    }

    /**
     * 按状态筛选
     * */
    @GetMapping("/list/status")
    public ResponseDetails listTablesByStatus(
            @RequestParam Long storefrontId,
            @RequestParam Integer status) {
        return ResponseDetails.ok().put("data", diningTableService.listTablesByStoreAndStatus(storefrontId, status));
    }


    /**
     * 查找可用餐桌
     * */
    @GetMapping("/available")
    public ResponseDetails findAvailableTables(
            @RequestParam Long storefrontId,
            @RequestParam Integer minCapacity) {
        return ResponseDetails.ok().put("data", diningTableService.findAvailableTables(storefrontId, minCapacity));
    }


    /**
     * 更新餐桌状态
     * */
    @PostMapping("/status")
    public ResponseDetails updateTableStatus(
            @RequestParam Long id,
            @RequestParam Long storefrontId,
            @RequestParam Integer status) {
        return ResponseDetails.ok().put("data", diningTableService.updateTableStatus(id, storefrontId, status));
    }
}
