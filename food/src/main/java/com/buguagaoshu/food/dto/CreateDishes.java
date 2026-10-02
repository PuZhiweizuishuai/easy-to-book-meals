package com.buguagaoshu.food.dto;

import jakarta.validation.constraints.NotNull;

import com.buguagaoshu.food.data.FileData;
import com.buguagaoshu.food.data.FoodTag;
import com.buguagaoshu.food.data.SpecGroup;
import com.buguagaoshu.food.enums.PriceType;
import com.buguagaoshu.food.valid.ListValue;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-27
 */
@Data
public class CreateDishes {

    /**
     * 所属门店ID（创建菜品时必填，用于门店数据隔离）
     */
    @NotNull(message = "门店ID不能为空")
    private Long storefrontId;

    private String name;

    /**
     * 菜品描述
     */
    private String description;

    /**
     * 首选显示照片
     *
     */
    private String topImage;

    /**
     * 其它图片数组 ["url1","url2"]
     *
     */
    private List<String> imageList;



    private BigDecimal unitPrice;

    /**
     * 计价方式，默认按份
     *
     */
    @ListValue(value = {0, 1, 2})
    private Integer priceType = PriceType.FIXED.code;


    private List<FoodTag> categoryIds;

    /**
     * 库存数量
     */
    private Long stock;


    /**
     * 菜品状态：0-下架，1-上架
     */
    @ListValue(value = {0, 1})
    private Integer status;


    private List<SpecGroup> specGroups;


    private List<FileData> fileDataList;
}
