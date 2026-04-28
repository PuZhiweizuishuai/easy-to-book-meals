package com.buguagaoshu.food.controller;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.buguagaoshu.food.dto.ResponseDetails;
import com.buguagaoshu.food.service.DishesService;
import org.springframework.web.bind.annotation.PathVariable;


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
    @RequestMapping("/{id}")
    public ResponseDetails getDishes(@PathVariable Long id) {
        return ResponseDetails.ok(dishesService.getDishes(id));
    }

}
