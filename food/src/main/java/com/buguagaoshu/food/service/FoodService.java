package com.buguagaoshu.food.service;


import com.buguagaoshu.food.cache.CommonlyUsedDataCache;
import com.buguagaoshu.food.config.WebConstant;
import com.buguagaoshu.food.data.BaseFood;
import com.buguagaoshu.food.data.FoodTag;
import com.buguagaoshu.food.data.FoodWithTag;
import com.buguagaoshu.food.dto.CreateDishes;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.BaseFoodRepository;
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

    private final FoodWithTagService foodWithTagService;

    public FoodService(BaseFoodRepository baseFoodRepository,
                       FoodWithTagService foodWithTagService) {
        this.baseFoodRepository = baseFoodRepository;
        this.foodWithTagService = foodWithTagService;
    }

    @Transactional(rollbackFor = Exception.class)
    public BaseFood saveFood(CreateDishes createDishes) {
        long time = System.currentTimeMillis();
        BaseFood baseFood = new BaseFood();
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

        // 如果当前页没有数据，直接返回，避免后续无意义的查询
        if (baseFoods.isEmpty()) {
            return baseFoods;
        }
        // 2. 提取当前页所有菜品的 ID
        List<Long> foodIds = baseFoods.getContent().stream()
                .map(BaseFood::getId)
                .collect(Collectors.toList());

        // 3. 批量查询这些菜品关联的所有标签（假设你的 Repository 支持 findByFoodIdIn）
        // 这一步只用 1 次 SQL 就能查出当前页所有菜品对应的标签
        List<FoodWithTag> allFoodWithTags = foodWithTagService.findByFoodIdIn(foodIds);

        // 4. 将标签数据按 foodId 分组，方便后续 O(1) 复杂度的匹配
        Map<Long, List<FoodWithTag>> tagMap = allFoodWithTags.stream()
                .collect(Collectors.groupingBy(FoodWithTag::getFoodId));

        // 5. 在内存中把标签数据填充到菜品对象中
        baseFoods.forEach(food -> {
            List<FoodWithTag> tags = tagMap.get(food.getId());
            if (tags != null) {
                List<FoodTag> foodTags = new ArrayList<>();
                for (FoodWithTag foodWithTag : tags) {
                    foodTags.add(CommonlyUsedDataCache.foodTagMap.get(foodWithTag.getTagId()));
                }
                food.setCategoryIds(foodTags);
            }
        });
        return baseFoods;
    }
}
