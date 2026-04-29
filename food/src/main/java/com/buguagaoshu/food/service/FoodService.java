package com.buguagaoshu.food.service;


import com.buguagaoshu.food.config.WebConstant;
import com.buguagaoshu.food.data.BaseFood;
import com.buguagaoshu.food.repository.BaseFoodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public BaseFood saveFood(BaseFood food) {
        food.setAllSales(0L);
        food.setMonthlySales(0L);
        food.setStatus(WebConstant.ENABLE);
        BaseFood savedFood = this.baseFoodRepository.save(food);
        // 关联分类信息
        if (food.getCategoryIds() != null && !food.getCategoryIds().isEmpty()) {
            foodWithTagService.associationTag(savedFood.getId(), food.getCategoryIds());
        }
        return savedFood;
    }

    public BaseFood getFood(Long id) {
        return baseFoodRepository.findById(id).get();
    }
}
