package com.buguagaoshu.food.service;


import cn.dev33.satoken.stp.StpUtil;
import com.buguagaoshu.food.cache.CommonlyUsedDataCache;
import com.buguagaoshu.food.config.WebConstant;
import com.buguagaoshu.food.data.BaseFood;
import com.buguagaoshu.food.data.FoodTag;
import com.buguagaoshu.food.data.FoodWithTag;
import com.buguagaoshu.food.data.ProductSku;
import com.buguagaoshu.food.dto.CreateDishes;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.BaseFoodRepository;
import com.buguagaoshu.food.repository.ProductSkuRepository;
import com.buguagaoshu.food.utils.JsonUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-26
 * 食物服务
 */
@Service
public class FoodService {
    private final BaseFoodRepository baseFoodRepository;
    private final ProductSkuRepository productSkuRepository;

    private final FoodWithTagService foodWithTagService;

    public FoodService(BaseFoodRepository baseFoodRepository,
                       ProductSkuRepository productSkuRepository,
                       FoodWithTagService foodWithTagService) {
        this.baseFoodRepository = baseFoodRepository;
        this.productSkuRepository = productSkuRepository;
        this.foodWithTagService = foodWithTagService;
    }

    @Transactional(rollbackFor = Exception.class)
    public BaseFood saveFood(CreateDishes createDishes) {
        long time = System.currentTimeMillis();
        BaseFood baseFood = new BaseFood();
        baseFood.setStorefrontId(createDishes.getStorefrontId());
        baseFood.setCreateUser(StpUtil.getLoginIdAsLong());
        baseFood.setName(createDishes.getName());
        baseFood.setDescription(createDishes.getDescription());
        baseFood.setTopImage(createDishes.getTopImage());

        try {
            baseFood.setImageList(JsonUtils.toJson(createDishes.getImageList()));
        } catch (Exception e) {
            baseFood.setImageList("[]");
        }
        baseFood.setPriceType(createDishes.getPriceType());
        baseFood.setCategoryIds(createDishes.getCategoryIds());
        baseFood.setStock(createDishes.getStock());
        baseFood.setStatus(createDishes.getStatus());
        baseFood.setAllSales(0L);
        baseFood.setMonthlySales(0L);
        // 设置基础价格
        baseFood.setBasePrice(createDishes.getUnitPrice());
        baseFood.setStatus(WebConstant.ENABLE);
        baseFood.setCreateTime(time);
        baseFood.setUpdateTime(time);
        BaseFood savedFood = this.baseFoodRepository.save(baseFood);
        // 关联分类信息
        if (baseFood.getCategoryIds() != null && !baseFood.getCategoryIds().isEmpty()) {
            foodWithTagService.associationTag(savedFood.getId(), baseFood.getCategoryIds());
            // 补全分类信息
            savedFood.setCategoryIds(foodWithTagService.completionTag(baseFood.getCategoryIds()));
        }

        return savedFood;
    }

    /**
     * 更新菜品基础信息
     * 只允许修改用户可编辑的属性，保护系统维护的属性不被篡改
     * 系统维护的属性：allSales、monthlySales、createTime（这些由系统自动管理，不允许用户修改）
     * */
    @Transactional(rollbackFor = Exception.class)
    public BaseFood updateFood(BaseFood updateData) {
        // 1. 查询数据库中的原始记录，确认菜品存在
        BaseFood existingFood = getFood(updateData.getId());

        // 2. 复制用户可修改的属性到原始对象上（在原有数据上修改，不删除重建）
        existingFood.setName(updateData.getName());
        existingFood.setDescription(updateData.getDescription());
        existingFood.setTopImage(updateData.getTopImage());
        existingFood.setImageList(updateData.getImageList());
        existingFood.setPriceType(updateData.getPriceType());
        existingFood.setBasePrice(updateData.getBasePrice());
        existingFood.setStock(updateData.getStock());
        existingFood.setStatus(updateData.getStatus());

        // 3. 更新系统维护的时间戳
        existingFood.setUpdateTime(System.currentTimeMillis());

        // 4. 保存修改
        BaseFood savedFood = baseFoodRepository.save(existingFood);

        // 5. 处理分类变更：先删除旧关联，再建立新关联
        List<FoodTag> newCategoryIds = updateData.getCategoryIds();
        foodWithTagService.updateCategoryTags(savedFood.getId(), newCategoryIds);

        // 6. 补全分类信息用于返回
        if (newCategoryIds != null && !newCategoryIds.isEmpty()) {
            savedFood.setCategoryIds(foodWithTagService.completionTag(newCategoryIds));
        } else {
            savedFood.setCategoryIds(new ArrayList<>());
        }

        return savedFood;
    }

    public BaseFood getFood(Long id) {
        try {
            return baseFoodRepository.findById(id).get();
        } catch (Exception e) {
            throw new BusinessException(404, "菜品不存在");
        }
    }

    /**
     * 顾客端查询门店菜单（仅上架菜品，按创建时间倒序）
     *
     * 注意：BaseFood.stock 是管理员设置时的"初始/计划库存"快照，
     * 下单时实际扣减的是 ProductSku.stock，因此 BaseFood.stock 不会随订单变化，
     * 直接展示会导致下单后库存数据不准确（仍然显示初始值）。
     *
     * 这里在返回前用各启用 SKU 的真实库存聚合出菜品可用库存覆盖该字段，
     * 规则：
     *   - 任一启用 SKU 不限库存(-1) → 菜品库存 -1（充足）
     *   - 否则 → 所有启用 SKU 库存之和（全部售罄则为 0）
     */
    public List<BaseFood> findStoreMenu(Long storefrontId) {
        List<BaseFood> foods = baseFoodRepository.findByStorefrontIdAndStatus(
                storefrontId, WebConstant.ENABLE);
        fillFoodTags(foods);
        fillAggregatedStock(foods);
        return foods;
    }

    /**
     * 按门店分页查询菜品（店主/店员管理端，含下架菜品）
     */
    public Page<BaseFood> findByStore(Long storefrontId, Integer page, Integer size, String key) {
        Page<BaseFood> baseFoods;

        if (StringUtils.hasLength(key)) {
            baseFoods = baseFoodRepository.findByStorefrontIdAndNameLike(
                    storefrontId,
                    "%" + key + "%",
                    PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createTime")))
            );
        } else {
            baseFoods = baseFoodRepository.findByStorefrontId(
                    storefrontId,
                    PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createTime"))));
        }

        fillFoodTags(baseFoods.getContent());
        return baseFoods;
    }

    public Page<BaseFood> findAll(Integer page, Integer size, String key) {
        Page<BaseFood> baseFoods;

        if (StringUtils.hasLength(key)) {
            baseFoods = baseFoodRepository.findByNameLike(
                    "%" + key + "%",
                    PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createTime")))
            );
        } else {
            baseFoods = baseFoodRepository.findAll(
                    PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createTime"))));
        }

        fillFoodTags(baseFoods.getContent());
        return baseFoods;
    }

    /**
     * 聚合各菜品的可用库存（基于启用 SKU 的实时库存）
     * 解决 BaseFood.stock 不随订单扣减导致菜单库存显示不准确的问题。
     */
    private void fillAggregatedStock(List<BaseFood> foods) {
        if (foods == null || foods.isEmpty()) {
            return;
        }
        List<Long> dishIds = foods.stream()
                .map(BaseFood::getId)
                .collect(Collectors.toList());

        // 一次性查询所有菜品的 SKU，避免 N+1
        List<ProductSku> allSkus = productSkuRepository.findByDishIdIn(dishIds);
        Map<Long, List<ProductSku>> skuMap = allSkus.stream()
                .collect(Collectors.groupingBy(ProductSku::getDishId));

        for (BaseFood food : foods) {
            List<ProductSku> skus = skuMap.get(food.getId());
            // 菜品暂无 SKU：保留管理员设置的初始库存
            if (skus == null || skus.isEmpty()) {
                continue;
            }
            // 仅统计启用中的 SKU（status == 1）
            List<ProductSku> enabledSkus = skus.stream()
                    .filter(s -> s.getStatus() != null && s.getStatus() == WebConstant.ENABLE)
                    .collect(Collectors.toList());
            if (enabledSkus.isEmpty()) {
                food.setStock(0L);
                continue;
            }
            // 任一启用 SKU 不限库存 → 整体展示为充足
            boolean anyUnlimited = enabledSkus.stream()
                    .anyMatch(s -> s.getStock() != null && s.getStock() == WebConstant.UNLIMITED_INVENTORY);
            if (anyUnlimited) {
                food.setStock(WebConstant.UNLIMITED_INVENTORY);
            } else {
                long total = enabledSkus.stream()
                        .mapToLong(s -> s.getStock() == null ? 0L : Math.max(0L, s.getStock()))
                        .sum();
                food.setStock(total);
            }
        }
    }

    /**
     * 为菜品列表批量填充分类标签信息（避免 N+1 查询）
     */
    private void fillFoodTags(List<BaseFood> foods) {
        if (foods == null || foods.isEmpty()) {
            return;
        }
        // 提取所有菜品的 ID
        List<Long> foodIds = foods.stream()
                .map(BaseFood::getId)
                .collect(Collectors.toList());

        // 批量查询这些菜品关联的所有标签，只用 1 次 SQL
        List<FoodWithTag> allFoodWithTags = foodWithTagService.findByFoodIdIn(foodIds);

        // 将标签数据按 foodId 分组，方便 O(1) 复杂度的匹配
        Map<Long, List<FoodWithTag>> tagMap = allFoodWithTags.stream()
                .collect(Collectors.groupingBy(FoodWithTag::getFoodId));

        // 在内存中把标签数据填充到菜品对象中
        foods.forEach(food -> {
            List<FoodWithTag> tags = tagMap.get(food.getId());
            if (tags != null) {
                List<FoodTag> foodTags = new ArrayList<>();
                for (FoodWithTag foodWithTag : tags) {
                    foodTags.add(CommonlyUsedDataCache.foodTagMap.get(foodWithTag.getTagId()));
                }
                food.setCategoryIds(foodTags);
            }
        });
    }
}

