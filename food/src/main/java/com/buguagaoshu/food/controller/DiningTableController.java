package com.buguagaoshu.food.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.buguagaoshu.food.data.DiningTable;
import com.buguagaoshu.food.dto.CreateDiningTableDto;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.dto.UpdateDiningTableDto;
import com.buguagaoshu.food.service.DiningTableService;
import com.buguagaoshu.food.utils.StoreAuthHelper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 餐桌控制器
 * <p>
 * 权限策略：
 * - 扫码查询：公开（顾客扫码点餐，通过二维码定位门店与餐桌）
 * - 查询类接口：需登录 + 门店 table:view 权限
 * - 操作类接口：需登录 + 门店 table:manage 权限
 */
@RestController
@RequestMapping("/api/dining-table")
@Validated
public class DiningTableController {
    private final DiningTableService diningTableService;
    private final StoreAuthHelper storeAuthHelper;

    public DiningTableController(DiningTableService diningTableService,
                                 StoreAuthHelper storeAuthHelper) {
        this.diningTableService = diningTableService;
        this.storeAuthHelper = storeAuthHelper;
    }

    /**
     * 通过二维码标识查询餐桌（公开，顾客扫码点餐入口）
     */
    @GetMapping("/qr/{qrCodeKey}")
    public ResponseDetails getTableByQrCode(@PathVariable String qrCodeKey) {
        DiningTable table = diningTableService.getTableByQrCode(qrCodeKey);
        return ResponseDetails.ok().put("data", table);
    }

    /**
     * 创建餐桌
     * */
    @SaCheckLogin
    @PostMapping("/add")
    public ResponseDetails createTable(@Valid @RequestBody CreateDiningTableDto dto) {
        storeAuthHelper.checkStorePermission(dto.getStorefrontId(), "table:manage");
        dto.setCreatUser(null);
        return ResponseDetails.ok().put("data", diningTableService.createTable(dto));
    }

    /**
     * 更新餐桌
     * */
    @SaCheckLogin
    @PostMapping("/update")
    public ResponseDetails updateTable(@Valid @RequestBody UpdateDiningTableDto dto) {
        storeAuthHelper.checkStorePermission(dto.getStorefrontId(), "table:manage");
        return ResponseDetails.ok().put("data", diningTableService.updateTable(dto));
    }

    /**
     * 逻辑删除餐桌
     * */
    @SaCheckLogin
    @PostMapping("/delete/{id}")
    public ResponseDetails deleteTable(@PathVariable Long id,
                                       @RequestParam Long storefrontId) {
        storeAuthHelper.checkStorePermission(storefrontId, "table:manage");
        diningTableService.deleteTable(id, storefrontId);
        return ResponseDetails.ok("删除成功");
    }


    /**
     * 查询单个餐桌
     * */
    @SaCheckLogin
    @GetMapping("/{id}")
    public ResponseDetails getTable(@PathVariable Long id,
                                    @RequestParam Long storefrontId) {
        storeAuthHelper.checkStorePermission(storefrontId, "table:view");
        return ResponseDetails.ok().put("data", diningTableService.getTable(id, storefrontId));
    }


    /**
     * 分页查询餐桌列表
     * */
    @SaCheckLogin
    @GetMapping("/list")
    public ResponseDetails listTables(
            @RequestParam Long storefrontId,
            @RequestParam(value = "page", required = false, defaultValue = "1")
            @Min(value = 1, message = "页码不能小于1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10")
            @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 100, message = "每页条数不能超过100") Integer size) {
        storeAuthHelper.checkStorePermission(storefrontId, "table:view");
        return ResponseDetails.ok().put("data", diningTableService.listTablesByStore(storefrontId, page, size));
    }


    /**
     * 查询全部餐桌
     * */
    @SaCheckLogin
    @GetMapping("/list/all")
    public ResponseDetails listAllTables(@RequestParam Long storefrontId) {
        storeAuthHelper.checkStorePermission(storefrontId, "table:view");
        return ResponseDetails.ok().put("data", diningTableService.listTablesByStore(storefrontId));
    }

    /**
     * 按状态筛选
     * */
    @SaCheckLogin
    @GetMapping("/list/status")
    public ResponseDetails listTablesByStatus(
            @RequestParam Long storefrontId,
            @RequestParam Integer status) {
        storeAuthHelper.checkStorePermission(storefrontId, "table:view");
        return ResponseDetails.ok().put("data", diningTableService.listTablesByStoreAndStatus(storefrontId, status));
    }


    /**
     * 查找可用餐桌
     * */
    @SaCheckLogin
    @GetMapping("/available")
    public ResponseDetails findAvailableTables(
            @RequestParam Long storefrontId,
            @RequestParam Integer minCapacity) {
        storeAuthHelper.checkStorePermission(storefrontId, "table:view");
        return ResponseDetails.ok().put("data", diningTableService.findAvailableTables(storefrontId, minCapacity));
    }


    /**
     * 更新餐桌状态
     * */
    @SaCheckLogin
    @PostMapping("/status")
    public ResponseDetails updateTableStatus(
            @RequestParam @NotNull Long id,
            @RequestParam @NotNull Long storefrontId,
            @RequestParam @NotNull Integer status) {
        storeAuthHelper.checkStorePermission(storefrontId, "table:manage");
        return ResponseDetails.ok().put("data", diningTableService.updateTableStatus(id, storefrontId, status));
    }
}
