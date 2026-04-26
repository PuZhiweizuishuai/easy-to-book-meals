package com.buguagaoshu.food.service;

import com.buguagaoshu.food.cache.CommonlyUsedDataCache;
import com.buguagaoshu.food.data.FoodTag;
import com.buguagaoshu.food.repository.FoodTagRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-26
 */
@Service
public class FoodTagService {
    private final FoodTagRepository foodTagRepository;

    public FoodTagService(FoodTagRepository foodTagRepository) {
        this.foodTagRepository = foodTagRepository;
    }

    public List<FoodTag> getAllFoodTag() {
        List<FoodTag> list = new ArrayList<>();
        foodTagRepository.findAll().forEach(list::add);
        return list;
    }


    public Map<Long, FoodTag> getFoodTagMap() {
        List<FoodTag> allFoodTag = getAllFoodTag();
        Map<Long, FoodTag> foodTagMap = new ConcurrentHashMap<>();
        for (FoodTag foodTag : allFoodTag) {
            foodTagMap.put(foodTag.getId(), foodTag);
        }
        return foodTagMap;
    }


    public FoodTag createTag(FoodTag foodTag) {
        foodTag.setCount(0);
        foodTag.setCreateTime(System.currentTimeMillis());
        foodTagRepository.save(foodTag);
        // 更新缓存
        CommonlyUsedDataCache.foodTagMap.put(foodTag.getId(), foodTag);
        return foodTag;
    }
}
