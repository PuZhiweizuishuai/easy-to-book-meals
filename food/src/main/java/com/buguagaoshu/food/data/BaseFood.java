package com.buguagaoshu.food.data;

import com.buguagaoshu.food.enums.PriceType;
import jakarta.persistence.*;
import lombok.Data;

import java.util.List;


/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-26
 * 基础食品信息
 * 价格与折扣信息应该在 SKU 中设置
 */
@Data
@Entity
@Table(name = "base_food_table")
public class BaseFood {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

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
    private String imageList;

    /**
     * 计价方式，默认按份
     * */
    private Integer priceType = PriceType.FIXED.code;


    /**
     * 接收前端分类数据
     * 展示分类数据
     * 分类数据单独查询
     * */
    @Transient
    private List<FoodTag> categoryIds;

    /**
     * 月销量
     */
    private Integer monthlySales;

    /**
     * 总销量
     */
    private Integer allSales;


    /**
     * 库存数量
     */
    private Integer stock;


    /**
     * 菜品状态：0-下架，1-上架
     */
    private Integer status;
}
