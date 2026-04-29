package com.buguagaoshu.food.service;


import com.buguagaoshu.food.config.WebConstant;
import com.buguagaoshu.food.data.BaseFood;
import com.buguagaoshu.food.dto.CreateDishes;
import com.buguagaoshu.food.repository.BaseFoodRepository;
import com.buguagaoshu.food.utils.JsonUtils;
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
    public BaseFood saveFood(CreateDishes createDishes) {
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
        baseFood.setStatus(WebConstant.ENABLE);
        BaseFood savedFood = this.baseFoodRepository.save(baseFood);
        // 关联分类信息
        if (baseFood.getCategoryIds() != null && !baseFood.getCategoryIds().isEmpty()) {
            foodWithTagService.associationTag(savedFood.getId(), baseFood.getCategoryIds());
        }
        return savedFood;
    }

    public BaseFood getFood(Long id) {
        return baseFoodRepository.findById(id).get();
    }
}
