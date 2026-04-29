package com.buguagaoshu.food.dto;

import com.buguagaoshu.food.data.FoodTag;
import com.buguagaoshu.food.data.SpecGroup;
import com.buguagaoshu.food.enums.PriceType;
import lombok.Data;

import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-27
 */
@Data
public class CreateDishes {

    private String name;

    /**
     * 菜品描述
     */
    private String description;

    /**
     * 首选显示照片
     * */
    private String topImage;

    /**
     * 其它图片数组 ["url1","url2"]
     * */
    private List<String> imageList;

    /**
     * 计价方式，默认按份
     * */
    private Integer priceType = PriceType.FIXED.code;



    private List<FoodTag> categoryIds;

    /**
     * 库存数量
     */
    private Long stock;


    /**
     * 菜品状态：0-下架，1-上架
     */
    private Integer status;


    private List<SpecGroup> specGroups;
}
