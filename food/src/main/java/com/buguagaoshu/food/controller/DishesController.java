package com.buguagaoshu.food.controller;


import com.buguagaoshu.food.dto.CreateDishes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
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


    @GetMapping("/list")
    @Validated
    public ResponseDetails list(@RequestParam(value = "page", required = false, defaultValue = "1")
                                @Min(value = 1, message = "页码不能小于1")
                                Integer page,

                                @RequestParam(value = "size", required = false, defaultValue = "10")
                                @Min(value = 1, message = "每页条数不能小于1")
                                @Max(value = 100, message = "每页条数不能超过100")
                                Integer size,

                                @RequestParam(value = "key", required = false)
                                    String ke) {
        return ResponseDetails.ok().put("data", dishesService.findBaseFoodList(page, size, ke));
    }


    /**
     * 增加菜品
     * */
    @PostMapping("/add")
    public ResponseDetails saveDishes(@Valid @RequestBody CreateDishes createDishes) {
        return ResponseDetails.ok().put("data", dishesService.createDishes(createDishes));
    }
}
