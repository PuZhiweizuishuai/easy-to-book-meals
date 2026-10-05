package com.buguagaoshu.food.controller.manage;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.ReportService;
import com.buguagaoshu.food.utils.StoreAuthHelper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 门店经营报表控制器（店主端）
 * <p>
 * 提供单日概览、历史/区间走势、畅销菜品榜三类数据。
 * 统计口径见 {@link ReportService}。
 * 需要 stats:store 权限（门店统计）。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-05
 */
@RestController
@RequestMapping("/api/manage/report")
@Validated
public class ReportController {

    private final ReportService reportService;
    private final StoreAuthHelper storeAuthHelper;

    public ReportController(ReportService reportService, StoreAuthHelper storeAuthHelper) {
        this.reportService = reportService;
        this.storeAuthHelper = storeAuthHelper;
    }

    /**
     * 指定日期经营概览（销售额、订单数、客单价、取消数、优惠金额、售出件数）
     *
     * @param date 日期 yyyy-MM-dd，默认今天
     */
    @SaCheckLogin
    @GetMapping("/overview")
    public ResponseDetails overview(@RequestParam @NotNull(message = "门店ID不能为空") Long storeId,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        storeAuthHelper.checkStorePermission(storeId, "stats:store");
        Map<String, Object> data = reportService.dailyOverview(storeId, date);
        return ResponseDetails.ok().put("data", data);
    }

    /**
     * 历史累计概览（全部已完成订单的销售额与订单数）
     */
    @SaCheckLogin
    @GetMapping("/total")
    public ResponseDetails total(@RequestParam @NotNull(message = "门店ID不能为空") Long storeId) {
        storeAuthHelper.checkStorePermission(storeId, "stats:store");
        return ResponseDetails.ok().put("data", reportService.totalOverview(storeId));
    }

    /**
     * 销售额 / 订单量走势
     *
     * @param start       开始日期 yyyy-MM-dd，默认近 30 天
     * @param end         结束日期 yyyy-MM-dd，默认今天
     * @param granularity day=按天（默认）；month=按月
     */
    @SaCheckLogin
    @GetMapping("/trend")
    public ResponseDetails trend(@RequestParam @NotNull(message = "门店ID不能为空") Long storeId,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                 @RequestParam(defaultValue = "day") String granularity) {
        storeAuthHelper.checkStorePermission(storeId, "stats:store");
        LocalDate today = LocalDate.now();
        LocalDate realEnd = end == null ? today : end;
        LocalDate realStart;
        if (start != null) {
            realStart = start;
        } else {
            realStart = "month".equalsIgnoreCase(granularity)
                    ? today.minusMonths(11).withDayOfMonth(1)
                    : today.minusDays(29);
        }
        return ResponseDetails.ok().put("data",
                reportService.trend(storeId, realStart, realEnd, granularity));
    }

    /**
     * 畅销菜品榜
     *
     * @param range day=日畅销（默认）；month=月畅销；all=历史畅销
     * @param date  基准日期，默认今天（all 时忽略）
     * @param limit 返回条数，默认 10
     */
    @SaCheckLogin
    @GetMapping("/top-dishes")
    public ResponseDetails topDishes(@RequestParam @NotNull(message = "门店ID不能为空") Long storeId,
                                     @RequestParam(defaultValue = "day") String range,
                                     @RequestParam(required = false)
                                     @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                     @RequestParam(defaultValue = "10")
                                     @Min(value = 1, message = "条数不能小于1")
                                     @Max(value = 100, message = "条数不能超过100") Integer limit) {
        storeAuthHelper.checkStorePermission(storeId, "stats:store");
        return ResponseDetails.ok().put("data",
                reportService.topDishes(storeId, range, date, limit));
    }
}
