package com.buguagaoshu.food.service;

import com.buguagaoshu.food.config.WebConstant;
import com.buguagaoshu.food.data.BaseFood;
import com.buguagaoshu.food.data.ProductSku;
import com.buguagaoshu.food.data.SpecGroup;
import com.buguagaoshu.food.data.SpecValue;
import com.buguagaoshu.food.enums.PriceType;
import com.buguagaoshu.food.repository.ProductSkuRepository;
import com.buguagaoshu.food.utils.JsonUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-27
 */
@Service
public class ProductSkuService {
    private final ProductSkuRepository productSkuRepository;

    public ProductSkuService(ProductSkuRepository productSkuRepository) {
        this.productSkuRepository = productSkuRepository;
    }

    public void saveAllProductSku(Iterable<ProductSku> productSkus) {
        productSkuRepository.saveAll(productSkus);
    }

    public List<ProductSku> findByDishId(Long dishId) {
        return productSkuRepository.findByDishId(dishId);
    }

    /**
     * 创建 SKU 列表
     * */
    public List<ProductSku> createSku(List<SpecGroup> specGroups, BaseFood baseFood) {
        List<ProductSku> skuList = new ArrayList<>();
        if (specGroups != null && !specGroups.isEmpty()) {
            // 准备规格值列表，按规格组分组
            List<List<Long>> specValueGroups = new ArrayList<>();
            for (SpecGroup specGroup : specGroups) {
                List<SpecValue> values = specGroup.getValues();
                if (values != null && !values.isEmpty()) {
                    List<Long> groupValues = new ArrayList<>();
                    for (SpecValue value : values) {
                        groupValues.add(value.getId());
                    }
                    specValueGroups.add(groupValues);
                }
            }

            // 生成笛卡尔积
            List<List<Long>> combinations = cartesianProduct(specValueGroups);

            // 创建 SKU
            for (List<Long> combination : combinations) {
                ProductSku sku = new ProductSku();
                sku.setDishId(baseFood.getId());
                sku.setUnitPrice(BigDecimal.ZERO);
                // unit 与 stock 相关，根据菜品的 priceType 来判断
                sku.setUnit(PriceType.getMsg(baseFood.getPriceType()));
                sku.setStock(0);
                sku.setDiscount(1.0);
                sku.setDiscountEndTime(0L);
                try {
                    sku.setSpecs(JsonUtils.toJson(combination));
                } catch (Exception e) {
                    sku.setSpecs("[]");
                }
                sku.setStatus(WebConstant.ENABLE);
                skuList.add(sku);
            }
        }
        return skuList;
    }

    /**
     * 计算笛卡尔积
     */
    private List<List<Long>> cartesianProduct(List<List<Long>> lists) {
        List<List<Long>> result = new ArrayList<>();
        if (lists.isEmpty()) {
            return result;
        }

        // 初始化结果列表
        for (Long value : lists.get(0)) {
            List<Long> list = new ArrayList<>();
            list.add(value);
            result.add(list);
        }

        // 逐步计算笛卡尔积
        for (int i = 1; i < lists.size(); i++) {
            List<List<Long>> newResult = new ArrayList<>();
            List<Long> currentList = lists.get(i);

            for (List<Long> existingList : result) {
                for (Long value : currentList) {
                    List<Long> newList = new ArrayList<>(existingList);
                    newList.add(value);
                    newResult.add(newList);
                }
            }

            result = newResult;
        }

        return result;
    }
}
