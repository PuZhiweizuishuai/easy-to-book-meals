package com.buguagaoshu.food.controller;


import com.buguagaoshu.food.cache.CommonlyUsedDataCache;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.FoodTagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/foodtag")
public class FoodTagController {
    private final FoodTagService foodTagService;

    public FoodTagController(FoodTagService foodTagService) {
        this.foodTagService = foodTagService;
    }

    @GetMapping("/map")
    public ResponseDetails getFoodTags() {
        return ResponseDetails.ok().put("data", CommonlyUsedDataCache.foodTagMap);
    }
}
