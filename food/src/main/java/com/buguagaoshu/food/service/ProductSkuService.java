package com.buguagaoshu.food.service;

import com.buguagaoshu.food.config.WebConstant;
import com.buguagaoshu.food.data.BaseFood;
import com.buguagaoshu.food.data.ProductSku;
import com.buguagaoshu.food.data.SpecGroup;
import com.buguagaoshu.food.data.SpecValue;
import com.buguagaoshu.food.dto.UpdateSpecGroupDto;
import com.buguagaoshu.food.enums.PriceType;
import com.buguagaoshu.food.repository.ProductSkuRepository;
import com.buguagaoshu.food.utils.JsonUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-27
 */
@Service
public class ProductSkuService {
    private final ProductSkuRepository productSkuRepository;

    private final SpecGroupService specGroupService;

    private final FoodService foodService;

    public ProductSkuService(ProductSkuRepository productSkuRepository,
                             SpecGroupService specGroupService,
                             FoodService foodService) {
        this.productSkuRepository = productSkuRepository;
        this.specGroupService = specGroupService;
        this.foodService = foodService;
    }

    public void saveAllProductSku(Iterable<ProductSku> productSkus) {
        productSkuRepository.saveAll(productSkus);
    }

    public List<ProductSku> findByDishId(Long dishId) {
        return productSkuRepository.findByDishId(dishId);
    }

    /**
     * 批量更新 SKU 列表
     * 只允许修改用户可编辑的属性，保护系统维护的属性不被篡改
     * 系统维护的属性：dishId（关联菜品）、specs（由规格值笛卡尔积生成，不应直接修改）
     *
     * 注意：更新采用在原有数据上修改的模式，不删除重建，避免订单数据读取异常
     * */
    public List<ProductSku> updateSku(List<ProductSku> updateSkuList) {
        List<ProductSku> updatedList = new ArrayList<>();

        // 空值判断，避免空指针
        if (updateSkuList == null || updateSkuList.isEmpty()) {
            return updatedList;
        }

        // ===================== 优化开始 =====================
        // 1. 提取所有要更新的 SKU ID 集合
        List<Long> skuIds = updateSkuList.stream()
                .map(ProductSku::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 2. 批量一次性查询所有存在的 SKU（只查 1 次数据库！）
        Iterable<ProductSku> existingSkuIterable = productSkuRepository.findAllById(skuIds);

        // 3. 把 Iterable 转成 Map<id, ProductSku>
        Map<Long, ProductSku> skuMap = new HashMap<>();
        for (ProductSku sku : existingSkuIterable) {
            // 不修改被锁定值
            if (sku.getStatus() == WebConstant.LOCK) {
                continue;
            }
            skuMap.put(sku.getId(), sku);
        }


        // 4. 循环校验 + 更新属性
        for (ProductSku updateData : updateSkuList) {
            Long skuId = updateData.getId();
            if (skuId == null) {
                continue;
            }

            // 从 map 中获取，不走数据库
            ProductSku existingSku = skuMap.get(skuId);
            if (existingSku == null) {
                continue;
            }

            // 5. 更新属性
            existingSku.setDescription(updateData.getDescription());
            existingSku.setUnitPrice(updateData.getUnitPrice());
            existingSku.setUnit(updateData.getUnit());
            existingSku.setStock(updateData.getStock());
            existingSku.setDiscount(updateData.getDiscount());
            existingSku.setDiscountEndTime(updateData.getDiscountEndTime());
            existingSku.setStatus(updateData.getStatus());

            updatedList.add(existingSku);
        }

        // 6. 批量保存
        if (!updatedList.isEmpty()) {
            productSkuRepository.saveAll(updatedList);
        }

        return updatedList;
    }

    /**
     * 创建 SKU 列表
     * */
    public List<ProductSku> createSku(List<SpecGroup> specGroups, BaseFood baseFood) {
        List<ProductSku> skuList = new ArrayList<>();
        if (specGroups == null || specGroups.isEmpty()) {
            return skuList;
        }

        Map<Long, String> specValueMap = new HashMap<>();
        List<List<Long>> specValueGroups = buildSpecValueGroups(specGroups, specValueMap);
        List<List<Long>> combinations = cartesianProduct(specValueGroups);

        for (List<Long> combination : combinations) {
            skuList.add(createSingleSku(combination, baseFood.getId(), baseFood, specValueMap));
        }
        return skuList;
    }


    /**
     * 调整 sku 列表，区别于 updateSku 方法
     * update方法主要用于修改已有 sku 列表
     * 调整 sku 列表方法主要用于当用户规格组发生变更后，使用新的规格组生成新的 sku 列表
     * 逻辑如下：
     *  1、使用 specGroupService 的 updateSpecGroups 方法保存并获取新的 规格组列表
     *  2、对新的规格组列表进行笛卡尔积生成新的 sku 列表初始值，参考 createSku 方法中 创建 SKU 部分代码
     *  3、使用 findByDishId 方法获取旧的 sku 列表
     *  4、对比获取到的新旧 sku 列表的 specs 值，如果 specs 相同，则不修改此 id 的 sku。
     *                                      如果新 sku 列表中存在 specs 不在旧 sku列表中的数据。则保存新的 sku
     *                                      如果旧的 sku 列表中存在 specs 新的 sku 列表中不存在的数据，则将旧 sku 列表中这条数据的 status 值设置为 WebConstant.LOCK 锁定状态，只读，不可修改编辑
     *  5、返回最终的 sku 列表
     *
     * */
    @Transactional(rollbackFor = Exception.class)
    public List<ProductSku> adjustSkuList(UpdateSpecGroupDto updateSpecGroupDto) {
        Long dishId = updateSpecGroupDto.getDishesId();

        // 1. 更新规格组并获取新的规格组列表
        List<SpecGroup> newSpecGroups = specGroupService.updateSpecGroups(updateSpecGroupDto);

        // 2. 基于新规格组生成新 SKU 列表初始值
        BaseFood baseFood = foodService.getFood(dishId);
        Map<Long, String> specValueMap = new HashMap<>();
        List<List<Long>> specValueGroups = buildSpecValueGroups(newSpecGroups, specValueMap);
        List<List<Long>> combinations = cartesianProduct(specValueGroups);

        List<ProductSku> newSkuList = new ArrayList<>();
        for (List<Long> combination : combinations) {
            newSkuList.add(createSingleSku(combination, dishId, baseFood, specValueMap));
        }

        // 3. 获取旧的 SKU 列表
        List<ProductSku> oldSkuList = findByDishId(dishId);

        // 4. 对比新旧 SKU 列表的 specs 值
        Map<String, ProductSku> oldSkuByHash = oldSkuList.stream()
                .collect(Collectors.toMap(ProductSku::getSpecsHash, sku -> sku, (a, b) -> a));

        Set<String> newSpecsHashSet = newSkuList.stream()
                .map(ProductSku::getSpecsHash)
                .collect(Collectors.toSet());

        List<ProductSku> result = new ArrayList<>();
        List<ProductSku> skusToSave = new ArrayList<>();

        // 遍历新 SKU 列表：specs 相同则保留旧 SKU，specs 不同则保存新 SKU
        for (ProductSku newSku : newSkuList) {
            if (oldSkuByHash.containsKey(newSku.getSpecsHash())) {
                result.add(oldSkuByHash.get(newSku.getSpecsHash()));
            } else {
                skusToSave.add(newSku);
                result.add(newSku);
            }
        }

        // 遍历旧 SKU 列表：specs 不在新列表中则锁定
        for (ProductSku oldSku : oldSkuList) {
            if (!newSpecsHashSet.contains(oldSku.getSpecsHash())) {
                oldSku.setStatus(WebConstant.LOCK);
                skusToSave.add(oldSku);
                result.add(oldSku);
            }
        }

        // 批量保存新增和状态变更的 SKU
        if (!skusToSave.isEmpty()) {
            productSkuRepository.saveAll(skusToSave);
        }

        // 5. 返回最终的 SKU 列表
        return result;
    }


    /**
     * 从规格组列表中提取规格值ID分组和规格值名称映射
     * specValueGroups: 按规格组分组的规格值ID列表，用于笛卡尔积计算
     * specValueMap: 规格值ID到名称的映射，用于 SKU 的 specNames 缓存
     * */
    private List<List<Long>> buildSpecValueGroups(List<SpecGroup> specGroups, Map<Long, String> specValueMap) {
        List<List<Long>> specValueGroups = new ArrayList<>();
        if (specGroups == null || specGroups.isEmpty()) {
            return specValueGroups;
        }
        for (SpecGroup specGroup : specGroups) {
            List<SpecValue> values = specGroup.getValues();
            if (values != null && !values.isEmpty()) {
                List<Long> groupValues = new ArrayList<>();
                for (SpecValue value : values) {
                    groupValues.add(value.getId());
                    specValueMap.put(value.getId(), value.getName());
                }
                specValueGroups.add(groupValues);
            }
        }
        return specValueGroups;
    }

    /**
     * 根据规格值组合创建单个 SKU
     * */
    private ProductSku createSingleSku(List<Long> combination, Long dishId, BaseFood baseFood, Map<Long, String> specValueMap) {
        ProductSku sku = new ProductSku();
        sku.setDishId(dishId);
        sku.setUnit(PriceType.getMsg(baseFood.getPriceType()));
        sku.setUnitPrice(baseFood.getBasePrice());
        sku.setStock(WebConstant.UNLIMITED_INVENTORY);
        sku.setDescription("");
        sku.setDiscount(1.0);
        sku.setDiscountEndTime(0L);
        sku.setSpecNames(specValueMap);
        sku.setSpecs(combination);
        sku.setSpecsHash(sku.getSpecs());
        sku.setStatus(WebConstant.ENABLE);
        return sku;
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
