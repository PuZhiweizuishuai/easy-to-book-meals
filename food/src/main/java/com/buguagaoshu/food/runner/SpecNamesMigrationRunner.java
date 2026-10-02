package com.buguagaoshu.food.runner;

import com.buguagaoshu.food.data.OrderItem;
import com.buguagaoshu.food.data.ProductSku;
import com.buguagaoshu.food.repository.OrderItemRepository;
import com.buguagaoshu.food.repository.ProductSkuRepository;
import com.buguagaoshu.food.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 规格名称数据迁移
 * <p>
 * 历史数据问题：ProductSku.specNames 原本缓存了整道菜所有规格值（如 {"1":"大份","5":"小份"}），
 * 导致订单详情展示成"大份/小份"。修复后只缓存该 SKU 实际选中的规格值。
 * <p>
 * 本 runner 在应用启动后幂等执行：按 SKU.specs 过滤 specNames，
 * OrderItem 则通过 skuId 关联 SKU.specs 过滤。已正确的数据不重复写入。
 *
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-10-02
 */
//@Component
//@Order(100)
public class SpecNamesMigrationRunner implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(SpecNamesMigrationRunner.class);

    private final ProductSkuRepository productSkuRepository;
    private final OrderItemRepository orderItemRepository;
    private final TransactionTemplate transactionTemplate;

    public SpecNamesMigrationRunner(ProductSkuRepository productSkuRepository,
                                    OrderItemRepository orderItemRepository,
                                    PlatformTransactionManager transactionManager) {
        this.productSkuRepository = productSkuRepository;
        this.orderItemRepository = orderItemRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                int skuFixed = fixSkus();
                int itemFixed = fixOrderItems();
                if (skuFixed > 0 || itemFixed > 0) {
                    logger.info("规格名称数据迁移完成：修正 SKU {} 条，OrderItem {} 条", skuFixed, itemFixed);
                }
            } catch (Exception e) {
                logger.error("规格名称数据迁移失败", e);
            }
        });
    }

    /**
     * 修正 SKU：按 specs 过滤 specNames，只保留选中的规格值
     */
    private int fixSkus() {
        List<ProductSku> all = new ArrayList<>();
        productSkuRepository.findAll().forEach(all::add);
        int fixed = 0;
        for (ProductSku sku : all) {
            if (sku.getSpecNames() == null || sku.getSpecs() == null) {
                continue;
            }
            try {
                Map<Long, String> namesMap = JsonUtils.toObject(sku.getSpecNames(),
                        new TypeReference<Map<Long, String>>() {});
                List<Long> specIds = JsonUtils.toObject(sku.getSpecs(),
                        new TypeReference<List<Long>>() {});
                if (namesMap == null || specIds == null || specIds.isEmpty()) {
                    continue;
                }
                Map<Long, String> filtered = new LinkedHashMap<>();
                for (Long id : specIds) {
                    String name = namesMap.get(id);
                    if (name != null) {
                        filtered.put(id, name);
                    }
                }
                // 原 Map 含 specs 之外的 key 才需要修正
                if (filtered.size() < namesMap.size()) {
                    sku.setSpecNames(filtered);
                    productSkuRepository.save(sku);
                    fixed++;
                }
            } catch (Exception e) {
                // 解析失败跳过该条
            }
        }
        return fixed;
    }

    /**
     * 修正 OrderItem：通过 skuId 找到 SKU 的 specs，过滤 specNames
     */
    private int fixOrderItems() {
        List<OrderItem> all = new ArrayList<>();
        orderItemRepository.findAll().forEach(all::add);
        if (all.isEmpty()) {
            return 0;
        }
        // 构建 skuId → specs 映射
        Map<Long, List<Long>> skuSpecsMap = new HashMap<>();
        List<ProductSku> allSkus = new ArrayList<>();
        productSkuRepository.findAll().forEach(allSkus::add);
        for (ProductSku sku : allSkus) {
            if (sku.getSpecs() == null) {
                continue;
            }
            try {
                List<Long> specIds = JsonUtils.toObject(sku.getSpecs(),
                        new TypeReference<List<Long>>() {});
                if (specIds != null) {
                    skuSpecsMap.put(sku.getId(), specIds);
                }
            } catch (Exception e) {
                // skip
            }
        }

        int fixed = 0;
        for (OrderItem item : all) {
            if (item.getSpecNames() == null) {
                continue;
            }
            List<Long> specIds = skuSpecsMap.get(item.getSkuId());
            if (specIds == null || specIds.isEmpty()) {
                continue;
            }
            try {
                Map<Long, String> namesMap = JsonUtils.toObject(item.getSpecNames(),
                        new TypeReference<Map<Long, String>>() {});
                if (namesMap == null) {
                    continue;
                }
                Map<Long, String> filtered = new LinkedHashMap<>();
                for (Long id : specIds) {
                    String name = namesMap.get(id);
                    if (name != null) {
                        filtered.put(id, name);
                    }
                }
                if (filtered.size() < namesMap.size()) {
                    item.setSpecNames(JsonUtils.toJson(filtered));
                    orderItemRepository.save(item);
                    fixed++;
                }
            } catch (Exception e) {
                // skip
            }
        }
        return fixed;
    }
}
