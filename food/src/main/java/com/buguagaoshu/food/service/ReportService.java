package com.buguagaoshu.food.service;

import com.buguagaoshu.food.data.OrderItem;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.OrderDataRepository;
import com.buguagaoshu.food.repository.OrderItemRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 门店经营报表服务
 * <p>
 * 设计要点（业界通行口径）：
 * <ul>
 *   <li><b>营业额只统计已完成订单</b>（status=4）：待接单/制作中不确认收入，取消与拒单不计入，
 *       避免把在途和流失订单算成营收</li>
 *   <li><b>客单价 = 已完成订单实付总额 / 已完成订单数</b>，实付已扣除菜品折扣、优惠券与手动减免</li>
 *   <li><b>按订单创建时间归属营业日</b>，口径稳定可复现</li>
 *   <li><b>畅销榜排除退菜明细</b>，按「销量（件/重量）」与「销售额」双指标排序</li>
 * </ul>
 * <p>
 * 聚合策略：先按时间区间拉取订单/明细，再在内存中分组聚合。
 * 单店日/月/年级数据量（万级以内）内存聚合远快于数据库 GROUP BY，
 * 且避免 native SQL 的日期函数方言差异（H2/MySQL 不通用），可移植性更好。
 * 时间区间长度做上限保护，避免一次加载过多数据。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-05
 */
@Service
public class ReportService {

    /** 单次查询最大天数，避免拉取过量数据 */
    private static final int MAX_RANGE_DAYS = 366;

    /** 明细批量查询的每批订单数：避免 IN 子句参数过多导致 SQL 过长 */
    private static final int ITEM_BATCH_SIZE = 500;

    /** 完成状态（与 OrderStatus.COMPLETED 一致） */
    private static final int STATUS_COMPLETED = 4;

    /** 取消状态（与 OrderStatus.CANCELLED 一致） */
    private static final int STATUS_CANCELLED = 5;

    private static final DateTimeFormatter DAY_KEY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");

    private final OrderDataRepository orderDataRepository;
    private final OrderItemRepository orderItemRepository;

    public ReportService(OrderDataRepository orderDataRepository,
                         OrderItemRepository orderItemRepository) {
        this.orderDataRepository = orderDataRepository;
        this.orderItemRepository = orderItemRepository;
    }

    // ==================== 单日概览 ====================

    /**
     * 指定日期的经营概览
     *
     * @param storeId 门店ID
     * @param date    日期（yyyy-MM-dd），为空表示今天
     */
    public Map<String, Object> dailyOverview(Long storeId, LocalDate date) {
        LocalDate day = date == null ? LocalDate.now() : date;
        long start = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long end = day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();

        List<Object[]> rows = orderDataRepository.findOrdersBetween(storeId, start, end);
        Map<String, Object> result = aggregateOrders(rows);
        result.put("date", day.format(DAY_KEY));

        // 售出菜品件数（排除退菜）
        List<Long> completedIds = completedOrderIds(rows);
        result.put("itemCount", countSoldItems(completedIds));

        // 历史累计（用于「历史销售额」展示）
        result.put("totalRevenue", nvl(orderDataRepository.sumTotalRevenue(storeId)));
        result.put("totalOrderCount", orderDataRepository.countByStoreIdAndStatus(storeId, STATUS_COMPLETED));
        return result;
    }

    /**
     * 历史累计概览（全部已完成订单）
     */
    public Map<String, Object> totalOverview(Long storeId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("revenue", nvl(orderDataRepository.sumTotalRevenue(storeId)));
        result.put("orderCount", orderDataRepository.countByStoreIdAndStatus(storeId, STATUS_COMPLETED));
        return result;
    }

    // ==================== 走势 ====================

    /**
     * 销售额 / 订单量走势
     *
     * @param storeId     门店ID
     * @param start       开始日期（含）
     * @param end         结束日期（含）
     * @param granularity day=按天；month=按月
     */
    public Map<String, Object> trend(Long storeId, LocalDate start, LocalDate end, String granularity) {
        boolean monthly = "month".equalsIgnoreCase(granularity);
        checkRange(start, end);

        long startMs = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endMs = end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        List<Object[]> rows = orderDataRepository.findOrdersBetween(storeId, startMs, endMs);

        // 预生成时间轴，保证没有订单的日期/月份也返回 0，折线图不会断点
        Map<String, Map<String, Object>> buckets = new LinkedHashMap<>();
        LocalDate cursor = monthly ? start.withDayOfMonth(1) : start;
        LocalDate stop = monthly ? end.withDayOfMonth(1) : end;
        while (!cursor.isAfter(stop)) {
            String key = monthly ? cursor.format(MONTH_KEY) : cursor.format(DAY_KEY);
            buckets.put(key, newBucket(monthly ? key : key.substring(5), key));
            cursor = monthly ? cursor.plusMonths(1) : cursor.plusDays(1);
        }

        for (Object[] row : rows) {
            long createTime = toLong(row[2]);
            LocalDate day = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(createTime),
                    ZoneId.systemDefault());
            String key = monthly ? day.format(MONTH_KEY) : day.format(DAY_KEY);
            Map<String, Object> bucket = buckets.get(key);
            if (bucket == null) {
                continue;
            }
            bucket.put("orderCount", toLong(bucket.get("orderCount")) + 1);
            int status = toInt(row[3]);
            if (status == STATUS_COMPLETED) {
                bucket.put("completedCount", toLong(bucket.get("completedCount")) + 1);
                bucket.put("revenue", nvl((BigDecimal) bucket.get("revenue")).add(nvl((BigDecimal) row[4])));
                bucket.put("discount", nvl((BigDecimal) bucket.get("discount"))
                        .add(nvl((BigDecimal) row[6]))
                        .add(nvl((BigDecimal) row[7]))
                        .add(nvl((BigDecimal) row[8])));
            } else if (status == STATUS_CANCELLED) {
                bucket.put("cancelledCount", toLong(bucket.get("cancelledCount")) + 1);
            }
        }

        List<Map<String, Object>> points = new ArrayList<>(buckets.values());
        // 收尾：金额保留两位、计算客单价
        for (Map<String, Object> p : points) {
            BigDecimal revenue = round2(nvl((BigDecimal) p.get("revenue")));
            long completed = toLong(p.get("completedCount"));
            p.put("revenue", revenue);
            p.put("discount", round2(nvl((BigDecimal) p.get("discount"))));
            p.put("avgOrderAmount", completed == 0 ? BigDecimal.ZERO
                    : revenue.divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("granularity", monthly ? "month" : "day");
        result.put("start", start.format(DAY_KEY));
        result.put("end", end.format(DAY_KEY));
        result.put("points", points);
        // 区间汇总，便于前端顶部展示
        result.put("summary", summarize(points));
        return result;
    }

    // ==================== 畅销榜 ====================

    /**
     * 畅销菜品榜
     *
     * @param storeId 门店ID
     * @param range   day=日畅销；month=月畅销；all=历史畅销
     * @param date    基准日期（day/month 使用；all 忽略）
     * @param limit   返回条数
     */
    public Map<String, Object> topDishes(Long storeId, String range, LocalDate date, int limit) {
        String normalized = (range == null || range.isBlank()) ? "day" : range.trim().toLowerCase();
        LocalDate base = date == null ? LocalDate.now() : date;
        long start;
        long end;
        switch (normalized) {
            case "day" -> {
                start = base.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                end = base.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            case "month" -> {
                LocalDate first = base.withDayOfMonth(1);
                start = first.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                end = first.plusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            case "all" -> {
                start = 0L;
                end = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            default -> throw new BusinessException(400, "不支持的统计范围: " + range);
        }

        List<Object[]> rows = orderDataRepository.findOrdersBetween(storeId, start, end);
        List<Long> completedIds = completedOrderIds(rows);
        List<OrderItem> items = loadNormalItems(completedIds);

        // 按菜品聚合（同一菜品不同规格合并统计）
        Map<Long, Map<String, Object>> dishMap = new HashMap<>();
        for (OrderItem item : items) {
            Map<String, Object> agg = dishMap.computeIfAbsent(item.getDishId(), k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("dishId", item.getDishId());
                m.put("dishName", item.getDishName());
                m.put("topImage", item.getTopImage());
                m.put("quantity", BigDecimal.ZERO);
                m.put("revenue", BigDecimal.ZERO);
                m.put("orderIds", new java.util.HashSet<Long>());
                return m;
            });
            agg.put("quantity", nvl((BigDecimal) agg.get("quantity")).add(nvl(item.getQuantity())));
            agg.put("revenue", nvl((BigDecimal) agg.get("revenue")).add(nvl(item.getTotalPrice())));
            if (item.getDishName() != null && agg.get("dishName") == null) {
                agg.put("dishName", item.getDishName());
            }
            @SuppressWarnings("unchecked")
            java.util.Set<Long> ids = (java.util.Set<Long>) agg.get("orderIds");
            ids.add(item.getOrderId());
        }

        List<Map<String, Object>> dishes = new ArrayList<>(dishMap.values());
        dishes.sort(Comparator
                .<Map<String, Object>, BigDecimal>comparing(d -> nvl((BigDecimal) d.get("quantity"))).reversed()
                .thenComparing(d -> nvl((BigDecimal) d.get("revenue")), Comparator.reverseOrder()));

        int size = Math.min(Math.max(limit, 1), 100);
        List<Map<String, Object>> top = new ArrayList<>();
        for (int i = 0; i < Math.min(size, dishes.size()); i++) {
            Map<String, Object> d = dishes.get(i);
            Map<String, Object> vo = new LinkedHashMap<>();
            vo.put("rank", i + 1);
            vo.put("dishId", d.get("dishId"));
            vo.put("dishName", d.get("dishName"));
            vo.put("topImage", d.get("topImage"));
            vo.put("quantity", round3(nvl((BigDecimal) d.get("quantity"))));
            vo.put("revenue", round2(nvl((BigDecimal) d.get("revenue"))));
            @SuppressWarnings("unchecked")
            java.util.Set<Long> ids = (java.util.Set<Long>) d.get("orderIds");
            vo.put("orderCount", ids.size());
            top.add(vo);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("range", normalized);
        result.put("date", normalized.equals("all") ? null : base.format(DAY_KEY));
        result.put("dishes", top);
        result.put("totalDishes", dishes.size());
        return result;
    }

    // ==================== 私有方法 ====================

    /**
     * 汇总一组订单行（概览/区间汇总共用）
     */
    private Map<String, Object> aggregateOrders(List<Object[]> rows) {
        Map<String, Object> result = new LinkedHashMap<>();
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        long orderCount = rows.size();
        long completedCount = 0;
        long cancelledCount = 0;
        long memberCount = 0;
        for (Object[] row : rows) {
            int status = toInt(row[3]);
            if (status == STATUS_COMPLETED) {
                completedCount++;
                revenue = revenue.add(nvl((BigDecimal) row[4]));
                discount = discount
                        .add(nvl((BigDecimal) row[6]))
                        .add(nvl((BigDecimal) row[7]))
                        .add(nvl((BigDecimal) row[8]));
                if (row[9] != null && !String.valueOf(row[9]).isBlank()) {
                    memberCount++;
                }
            } else if (status == STATUS_CANCELLED) {
                cancelledCount++;
            }
        }
        result.put("revenue", round2(revenue));
        result.put("orderCount", orderCount);
        result.put("completedCount", completedCount);
        result.put("cancelledCount", cancelledCount);
        result.put("memberOrderCount", memberCount);
        result.put("discountAmount", round2(discount));
        result.put("avgOrderAmount", completedCount == 0 ? BigDecimal.ZERO
                : round2(revenue).divide(BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP));
        return result;
    }

    /** 对走势各点做区间汇总 */
    private Map<String, Object> summarize(List<Map<String, Object>> points) {
        BigDecimal revenue = BigDecimal.ZERO;
        long orderCount = 0;
        long completedCount = 0;
        for (Map<String, Object> p : points) {
            revenue = revenue.add(nvl((BigDecimal) p.get("revenue")));
            orderCount += toLong(p.get("orderCount"));
            completedCount += toLong(p.get("completedCount"));
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("revenue", round2(revenue));
        summary.put("orderCount", orderCount);
        summary.put("completedCount", completedCount);
        summary.put("avgOrderAmount", completedCount == 0 ? BigDecimal.ZERO
                : round2(revenue).divide(BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP));
        return summary;
    }

    private Map<String, Object> newBucket(String label, String key) {
        Map<String, Object> bucket = new LinkedHashMap<>();
        bucket.put("label", label);
        bucket.put("date", key);
        bucket.put("revenue", BigDecimal.ZERO);
        bucket.put("discount", BigDecimal.ZERO);
        bucket.put("orderCount", 0L);
        bucket.put("completedCount", 0L);
        bucket.put("cancelledCount", 0L);
        bucket.put("avgOrderAmount", BigDecimal.ZERO);
        return bucket;
    }

    private List<Long> completedOrderIds(List<Object[]> rows) {
        List<Long> ids = new ArrayList<>();
        for (Object[] row : rows) {
            if (toInt(row[3]) == STATUS_COMPLETED) {
                ids.add(toLong(row[0]));
            }
        }
        return ids;
    }

    private BigDecimal countSoldItems(List<Long> completedIds) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : loadNormalItems(completedIds)) {
            total = total.add(nvl(item.getQuantity()));
        }
        return round3(total);
    }

    /**
     * 分批加载订单明细：订单量大时拆批查询，避免 IN 子句参数过多
     */
    private List<OrderItem> loadNormalItems(List<Long> completedIds) {
        if (completedIds.isEmpty()) {
            return List.of();
        }
        if (completedIds.size() <= ITEM_BATCH_SIZE) {
            return orderItemRepository.findNormalItemsByOrderIds(completedIds);
        }
        List<OrderItem> all = new ArrayList<>();
        for (int i = 0; i < completedIds.size(); i += ITEM_BATCH_SIZE) {
            List<Long> batch = completedIds.subList(i, Math.min(i + ITEM_BATCH_SIZE, completedIds.size()));
            all.addAll(orderItemRepository.findNormalItemsByOrderIds(batch));
        }
        return all;
    }

    private void checkRange(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new BusinessException(400, "统计起止日期不能为空");
        }
        if (end.isBefore(start)) {
            throw new BusinessException(400, "结束日期不能早于开始日期");
        }
        if (start.plusDays(MAX_RANGE_DAYS).isBefore(end)) {
            throw new BusinessException(400, "统计区间不能超过 " + MAX_RANGE_DAYS + " 天");
        }
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal round2(BigDecimal value) {
        return nvl(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal round3(BigDecimal value) {
        return nvl(value).setScale(3, RoundingMode.HALF_UP);
    }

    private static long toLong(Object value) {
        return value instanceof Number n ? n.longValue() : 0L;
    }

    private static int toInt(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    /** 今日零点时间戳（供外部复用） */
    public static long todayStart() {
        return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /** 当日结束时间戳（次日零点，左闭右开） */
    public static long todayEnd() {
        return LocalTime.MIDNIGHT.atDate(LocalDate.now().plusDays(1))
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
