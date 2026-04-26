package com.buguagaoshu.food.service;

import com.buguagaoshu.food.cache.CommonlyUsedDataCache;
import com.buguagaoshu.food.data.FoodTag;
import com.buguagaoshu.food.data.FoodWithTag;
import com.buguagaoshu.food.repository.FoodWithTagRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-26
 */
@Service
public class FoodWithTagService {
    private final FoodWithTagRepository foodWithTagRepository;

    public FoodWithTagService(FoodWithTagRepository foodWithTagRepository) {
        this.foodWithTagRepository = foodWithTagRepository;
    }

    /**
     * 关联分类信息
     * */
    public void associationTag(Long foodId, List<FoodTag> tags) {
        long time = System.currentTimeMillis();
        List<FoodWithTag> foodWithTagList = new ArrayList<>();
        tags.forEach(tag -> {
            // 如果表中有这个标签数据才关联，没有则不关联
            if (CommonlyUsedDataCache.foodTagMap.containsKey(tag.getId())) {
                FoodWithTag tagWithTag = new FoodWithTag();
                tagWithTag.setFoodId(foodId);
                tagWithTag.setTagId(tag.getId());
                tagWithTag.setCreateTime(time);
                foodWithTagList.add(tagWithTag);
            }
        });
        foodWithTagRepository.saveAll(foodWithTagList);
    }
}
