package com.buguagaoshu.food.data;

import com.buguagaoshu.food.enums.PriceType;
import com.buguagaoshu.food.valid.ListValue;
import jakarta.persistence.*;
import lombok.Data;


import java.math.BigDecimal;
import java.util.List;


/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * @create 2026-04-26
 * 基础食品信息
 * 价格与折扣信息应该在 SKU 中设置
 */
@Data
@Entity
@Table(name = "base_food_table", indexes = {
        @Index(name = "idx_base_food_status", columnList = "status"),
        @Index(name = "idx_base_food_monthlySales", columnList = "monthlySales"),
        @Index(name = "idx_base_food_allSales", columnList = "allSales")
})
public class BaseFood {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    /**
     * 菜品描述
     */
    @Column(columnDefinition = "longtext")
    private String description;

    /**
     * 首选显示照片
     * */
    private String topImage;

    /**
     * 其它图片数组 ["url1","url2"]
     * */
    @Column(columnDefinition = "text")
    private String imageList;

    /**
     * 计价方式，默认按份
     * */
    @ListValue(value = {0, 1, 2})
    private Integer priceType = PriceType.FIXED.code;

    /**
     * 基础价格
     * */
    private BigDecimal basePrice;


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
    private Long monthlySales;

    /**
     * 总销量
     */
    private Long allSales;


    /**
     * 库存数量
     * -1 不限制库存
     */
    private Long stock;


    /**
     * 菜品状态：0-下架，1-上架
     */
    @ListValue(value = {0, 1})
    private Integer status;


    private Long createTime;


    private Long updateTime;
}
