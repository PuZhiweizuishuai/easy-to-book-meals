package com.buguagaoshu.food.controller;


import com.buguagaoshu.food.dto.CreateDishes;
import org.springframework.web.bind.annotation.*;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.DishesService;


/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-27
 * 菜品控制器
 */
@RestController
@RequestMapping("/api/dishes")
public class DishesController {
    private final DishesService dishesService;

    public DishesController(DishesService dishesService) {
        this.dishesService = dishesService;
    }


    /**
     * 获取该菜品的菜品信息与所有 SKU
     */
    @GetMapping("/{id}")
    public ResponseDetails getDishes(@PathVariable Long id) {
        return ResponseDetails.ok().put("data", dishesService.getDishes(id));
    }


    @PostMapping("/add")
    public ResponseDetails saveDishes(@RequestBody CreateDishes createDishes) {
        return ResponseDetails.ok().put("data", dishesService.createDishes(createDishes));
    }
}
