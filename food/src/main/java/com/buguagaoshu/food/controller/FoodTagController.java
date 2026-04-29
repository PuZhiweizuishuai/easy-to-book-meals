package com.buguagaoshu.food.controller;


import com.buguagaoshu.food.cache.CommonlyUsedDataCache;
import com.buguagaoshu.food.data.FoodTag;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.FoodTagService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/foodtag")
public class FoodTagController {
    private final FoodTagService foodTagService;

    public FoodTagController(FoodTagService foodTagService) {
        this.foodTagService = foodTagService;
    }

    @GetMapping("/map")
    public ResponseDetails getFoodTagMap() {
        return ResponseDetails.ok().put("data", CommonlyUsedDataCache.foodTagMap);
    }

    @GetMapping("/list")
    public ResponseDetails getFoodTagList() {
        return ResponseDetails.ok().put("data", CommonlyUsedDataCache.foodTagList);
    }


    @PostMapping("/add")
    public ResponseDetails createFoodTag(@RequestBody FoodTag foodTag) {
        return ResponseDetails.ok().put("data", foodTagService.createTag(foodTag));
    }

    @PostMapping("/update")
    public ResponseDetails updateFoodTag(@RequestBody FoodTag foodTag) {
        return ResponseDetails.ok().put("data", foodTagService.updateTag(foodTag));
    }

    @PostMapping("/delete/{id}")
    public ResponseDetails deleteTag(@PathVariable Long id) {
        return ResponseDetails.ok().put("data", foodTagService.deleteTag(id));
    }
}
