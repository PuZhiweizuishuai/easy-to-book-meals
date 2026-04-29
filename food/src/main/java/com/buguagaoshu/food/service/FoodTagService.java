package com.buguagaoshu.food.service;

import com.buguagaoshu.food.cache.CommonlyUsedDataCache;
import com.buguagaoshu.food.data.FoodTag;
import com.buguagaoshu.food.enums.ReturnCodeEnum;
import com.buguagaoshu.food.exception.BusinessException;
import com.buguagaoshu.food.repository.FoodTagRepository;
import org.springframework.stereotype.Service;

import java.util.*;
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
        CommonlyUsedDataCache.foodTagList.add(foodTag);
        CommonlyUsedDataCache.foodTagList.sort(Comparator.comparingInt(FoodTag::getSort));
        return foodTag;
    }

    public FoodTag updateTag(FoodTag foodTag) {
        Optional<FoodTag> dbFoodTag = foodTagRepository.findById(foodTag.getId());
        if (!dbFoodTag.isPresent()) {
            throw new BusinessException(ReturnCodeEnum.NOT_FOUND.getCode(), "更新的食品标签不存在！");
        }
        dbFoodTag.get().setName(foodTag.getName());
        dbFoodTag.get().setSort(foodTag.getSort());
        FoodTag savedFoodTag = foodTagRepository.save(dbFoodTag.get());
        // 更新缓存
        CommonlyUsedDataCache.foodTagMap.put(foodTag.getId(), savedFoodTag);
        // 找到并更新列表中的对应项
        for (int i = 0; i < CommonlyUsedDataCache.foodTagList.size(); i++) {
            if (CommonlyUsedDataCache.foodTagList.get(i).getId().equals(foodTag.getId())) {
                CommonlyUsedDataCache.foodTagList.set(i, savedFoodTag);
                break;
            }
        }
        CommonlyUsedDataCache.foodTagList.sort(Comparator.comparingInt(FoodTag::getSort));
        return savedFoodTag;
    }


    public boolean deleteTag(Long id) {
        foodTagRepository.deleteById(id);
        // 更新缓存
        CommonlyUsedDataCache.foodTagMap.remove(id);
        CommonlyUsedDataCache.foodTagList.removeIf(tag -> tag.getId().equals(id));
        return true;
    }
}
